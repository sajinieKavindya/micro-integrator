/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * you may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.micro.integrator.initializer.deployment.application.deployer;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axis2.deployment.DeploymentException;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.micro.application.deployer.AppDeployerConstants;
import org.wso2.micro.application.deployer.AppDeployerUtils;
import org.wso2.micro.application.deployer.CarbonApplication;
import org.wso2.micro.application.deployer.config.ApplicationConfiguration;
import org.wso2.micro.application.deployer.config.Artifact;
import org.wso2.micro.application.deployer.handler.AppDeploymentHandler;
import org.wso2.micro.core.util.CarbonException;
import org.wso2.micro.core.util.FileManipulator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.xml.stream.XMLStreamException;

/**
 * Carbon Application deployer to deploy synapse artifacts
 */
public class CAppDeploymentManager {

    private static final Log log = LogFactory.getLog(CAppDeploymentManager.class);

    private AxisConfiguration axisConfiguration;
    private List<AppDeploymentHandler> appDeploymentHandlers;
    private static Map<String, ArrayList<CarbonApplication>> tenantcAppMap;
    private Map<String, HashMap<String, Exception>> tenantfaultycAppMap;

    private static CAppDeploymentManager instance = new CAppDeploymentManager();

    private CAppDeploymentManager() {
        this.appDeploymentHandlers = new ArrayList<AppDeploymentHandler>();
        tenantcAppMap = new ConcurrentHashMap<String, ArrayList<CarbonApplication>>();
    }

    public void init(AxisConfiguration axisConfiguration) {
        this.axisConfiguration = axisConfiguration;
    }

    public static CAppDeploymentManager getInstance() {
        return instance;
    }

    public void deploy(String artifactPath, AxisConfiguration axisConfig) throws CarbonException {

        String cAppSrcDir = axisConfiguration.getRepository().getPath() + AppDeployerConstants.CARBON_APPS;
        File cAppDir = new File(cAppSrcDir);

        String archPathToProcess = AppDeployerUtils.formatPath(artifactPath);
        String cAppName = archPathToProcess.substring(archPathToProcess.lastIndexOf('/') + 1);

        if (!isCAppArchiveFile(cAppName)) {
            log.warn("Only .car files are processed. Hence " + cAppName + " will be ignored");
            return;
        }
        if (log.isDebugEnabled()) {
            log.debug("Carbon Application detected : " + cAppName);
        }

        String targetCAppPath = cAppDir + File.separator + cAppName;

        try {
            // Extract to temporary location
            String tempExtractedDirPath = AppDeployerUtils.extractCarbonApp(targetCAppPath);

            // Build the app configuration by providing the artifacts.xml path
            ApplicationConfiguration appConfig = new ApplicationConfiguration(tempExtractedDirPath +
                    ApplicationConfiguration.ARTIFACTS_XML);

            // If we don't have features (artifacts) for this server, ignore
            if (appConfig.getApplicationArtifact().getDependencies().size() == 0) {
                log.warn("No artifacts found to be deployed in this server. " +
                        "Ignoring Carbon Application : " + cAppName);
                return;
            }

            CarbonApplication currentApp = new CarbonApplication();
            currentApp.setAppFilePath(targetCAppPath);
            currentApp.setExtractedPath(tempExtractedDirPath);
            currentApp.setAppConfig(appConfig);

            // Set App Name
            String appName = appConfig.getAppName();
            if (appName == null) {
                log.warn("No application name found in Carbon Application : " + cAppName + ". Using " +
                        "the file name as the application name");
                appName = cAppName.substring(0, cAppName.lastIndexOf('.'));
            }
            // to support multiple capp versions, we check app name with version
            if (appExists(appConfig.getAppNameWithVersion(), axisConfig)) {
                String msg = "Carbon Application : " + appConfig.getAppNameWithVersion() + " already exists. Two applications " +
                        "can't have the same Id. Deployment aborted.";
                log.error(msg);
                throw new CarbonException(msg);
            }
            currentApp.setAppName(appName);

            // Set App Version
            String appVersion = appConfig.getAppVersion();
            if (appVersion != null && !("").equals(appVersion)) {
                currentApp.setAppVersion(appVersion);
            }

            // deploy sub artifacts of this cApp
            this.searchArtifacts(currentApp.getExtractedPath(), currentApp);

            if (isArtifactReadyToDeploy(currentApp.getAppConfig().getApplicationArtifact())) {
                // Now ready to deploy
                // send the CarbonApplication instance through the handler chain
                for (AppDeploymentHandler appDeploymentHandler : appDeploymentHandlers) {
                    appDeploymentHandler.deployArtifacts(currentApp, axisConfiguration);
                }
            } else {
                log.error("Some dependencies were not satisfied in cApp:" +
                        currentApp.getAppNameWithVersion() +
                        "Check whether all dependent artifacts are included in cApp file: " +
                        targetCAppPath);

                deleteExtractedCApp(currentApp.getExtractedPath());
                return;
            }

            // Deployment Completed
            currentApp.setDeploymentCompleted(true);
            this.addCarbonApp(String.valueOf(AppDeployerUtils.getTenantId()), currentApp);
            log.info("Successfully Deployed Carbon Application : " + currentApp.getAppNameWithVersion() +
                    AppDeployerUtils.getTenantIdLogString(AppDeployerUtils.getTenantId()));

        } catch (DeploymentException e) {
            log.error("Error occurred while deploying the Carbon application: " + targetCAppPath , e);
        }
    }

    /**
     * Check whether there is an already existing Carbon application with the given name.
     * Use app name with version to support multiple capp versions
     *
     * @param newAppNameWithVersion - name of the new app
     * @param axisConfig - AxisConfiguration instance
     * @return - true if exits
     */
    private boolean appExists(String newAppNameWithVersion, AxisConfiguration axisConfig) {
        String tenantId = AppDeployerUtils.getTenantIdString();
        CarbonApplication appToRemove = null;
        for (CarbonApplication carbonApp : getCarbonApps(tenantId)) {
            if (newAppNameWithVersion.equals(carbonApp.getAppNameWithVersion())) {
                if (carbonApp.isDeploymentCompleted()) {
                    return true;
                } else {
                    appToRemove = carbonApp;
                    break;
                }
            }
        }
        if (appToRemove != null) {
            undeployCarbonApp(appToRemove, axisConfig);
        }
        return false;
    }


    /**
     * Deletes a directory given it's path.
     *
     * @param path the path of the directory to be deleted
     */
    private void deleteExtractedCApp(String path) {
        try {
            FileUtils.deleteDirectory(new File(path));
        } catch (IOException e) {
            log.warn("Unable to locate: " + path);
        }
    }

    /**
     * Add a new cApp for a particular tenant. If there are no cApps currently, create a new
     * ArrayList and add the new cApp.
     *
     * @param tenantId - tenant id of the cApp
     * @param carbonApp - CarbonApplication instance
     */
    public void addCarbonApp(String tenantId, CarbonApplication carbonApp) {
        ArrayList<CarbonApplication> cApps;
        synchronized (tenantId.intern()) {
            cApps = tenantcAppMap.get(tenantId);
            if (null == cApps) {
                cApps = new ArrayList<CarbonApplication>();
                tenantcAppMap.put(tenantId, cApps);
            }
        }
        // don't add the cApp if it already exists
        for (CarbonApplication cApp : cApps) {
            String appNameWithVersion = cApp.getAppNameWithVersion();
            if (appNameWithVersion != null && appNameWithVersion.equals(carbonApp.getAppNameWithVersion())) {
                return;
            }
        }
        cApps.add(carbonApp);
    }

    /**
     * Get the list of CarbonApplications for the give tenant id. If the list is null, return
     * an empty ArrayList
     *
     * @param tenantId - tenant id to find cApps
     * @return - list of tenant cApps
     */
    public static ArrayList<CarbonApplication> getCarbonApps(String tenantId) {
        ArrayList<CarbonApplication> cApps = tenantcAppMap.get(tenantId);
        if (null == cApps) {
            cApps = new ArrayList<CarbonApplication>();
        }
        return cApps;
    }

    /**
     * Checks whether a given file is a jar or an aar file.
     *
     * @param filename file to check
     * @return Returns boolean.
     */
    public static boolean isCAppArchiveFile(String filename) {
        return (filename.endsWith(".car"));
    }

    /**
     * Function to register application deployers
     *
     * @param handler - app deployer which implements the AppDeploymentHandler interface
     */
    public synchronized void registerDeploymentHandler(AppDeploymentHandler handler) {
        appDeploymentHandlers.add(handler);
    }

    /**
     * Deploys all artifacts under a root artifact..
     *
     * @param rootDirPath - root dir of the extracted artifact
     * @param parentApp - capp instance
     * @throws org.wso2.micro.core.util.CarbonException - on error
     */
    private void searchArtifacts(String rootDirPath, CarbonApplication parentApp) throws CarbonException {
        File extractedDir = new File(rootDirPath);
        File[] allFiles = extractedDir.listFiles();
        if (allFiles == null) {
            return;
        }

        // list to keep all artifacts
        List<Artifact> allArtifacts = new ArrayList<Artifact>();

        // search for all directories under the extracted path
        for (File artifactDirectory : allFiles) {
            if (!artifactDirectory.isDirectory()) {
                continue;
            }

            String directoryPath = AppDeployerUtils.formatPath(artifactDirectory.getAbsolutePath());
            String artifactXmlPath =  directoryPath + File.separator + Artifact.ARTIFACT_XML;

            File f = new File(artifactXmlPath);
            // if the artifact.xml not found, ignore this dir
            if (!f.exists()) {
                continue;
            }

            Artifact artifact = null;
            InputStream xmlInputStream = null;
            try {
                xmlInputStream = new FileInputStream(f);
                artifact = this.buildAppArtifact(parentApp, xmlInputStream);
            } catch (FileNotFoundException e) {
                handleException("artifacts.xml File cannot be loaded from " + artifactXmlPath, e);
            } finally {
                if (xmlInputStream != null) {
                    try {
                        xmlInputStream.close();
                    } catch (IOException e) {
                        log.error("Error while closing input stream.", e);
                    }
                }
            }

            if (artifact == null) {
                return;
            }
            artifact.setExtractedPath(directoryPath);
            allArtifacts.add(artifact);
        }
        Artifact appArtifact = parentApp.getAppConfig().getApplicationArtifact();
        buildDependencyTree(appArtifact, allArtifacts);
    }

    /**
     * Builds the artifact from the given input steam and adds it as a dependency in the provided
     * parent carbon application.
     *
     * @param parentApp - parent application
     * @param artifactXmlStream - xml input stream of the artifact.xml
     * @return - Artifact instance if successfull. otherwise null..
     * @throws CarbonException - error while building
     */
    public Artifact buildAppArtifact(CarbonApplication parentApp, InputStream artifactXmlStream)
            throws CarbonException {
        Artifact artifact = null;
        try {
            OMElement artElement = new StAXOMBuilder(artifactXmlStream).getDocumentElement();

            if (Artifact.ARTIFACT.equals(artElement.getLocalName())) {
                artifact = AppDeployerUtils.populateArtifact(artElement);
            } else {
                log.error("artifact.xml is invalid. Parent Application : "
                        + parentApp.getAppNameWithVersion());
                return null;
            }
        } catch (XMLStreamException e) {
            handleException("Error while parsing the artifact.xml file ", e);
        }

        if (artifact == null || artifact.getName() == null) {
            log.error("Invalid artifact found in Carbon Application : " + parentApp.getAppNameWithVersion());
            return null;
        }
        return artifact;
    }

    /**
     * Checks whether the given cApp artifact is complete with all it's dependencies. Recursively
     * checks all it's dependent artifacts as well..
     *
     * @param rootArtifact - artifact to check
     * @return true if ready, else false
     */
    private boolean isArtifactReadyToDeploy(Artifact rootArtifact) {
        if (rootArtifact == null) {
            return false;
        }
        boolean isReady = true;
        for (Artifact.Dependency dep : rootArtifact.getDependencies()) {
            isReady = isArtifactReadyToDeploy(dep.getArtifact());
            if (!isReady) {
                return false;
            }
        }
        if (rootArtifact.unresolvedDepCount > 0) {
            isReady = false;
        }
        return isReady;
    }

    /**
     * If the given artifact is a dependent artifact for the rootArtifact, include it as
     * the actual dependency. The existing one is a dummy one. So remove it. Do this recursively
     * for the dependent artifacts as well..
     *
     * @param rootArtifact - root to start search
     * @param allArtifacts - all artifacts found under current cApp
     */
    public void buildDependencyTree(Artifact rootArtifact, List<Artifact> allArtifacts) {
        for (Artifact.Dependency dep : rootArtifact.getDependencies()) {
            for (Artifact temp : allArtifacts) {
                if (dep.getName().equals(temp.getName())) {
                    String depVersion = dep.getVersion();
                    String attVersion = temp.getVersion();
                    if ((depVersion == null && attVersion == null) ||
                            (depVersion != null && depVersion.equals(attVersion))) {
                        dep.setArtifact(temp);
                        rootArtifact.unresolvedDepCount--;
                        break;
                    }
                }
            }

            // if we've found the dependency, check for it's dependencies as well..
            if (dep.getArtifact() != null) {
                buildDependencyTree(dep.getArtifact(), allArtifacts);
            }
        }
    }

    private void handleException(String msg, Exception e) throws CarbonException {
        log.error(msg, e);
        throw new CarbonException(msg, e);
    }

    /**
     * Undeploy the provided carbon App by sending it through the registered undeployment handler
     * chain..
     * @param carbonApp - CarbonApplication instance
     * @param axisConfig - AxisConfiguration of the current tenant
     */
    public void undeployCarbonApp(CarbonApplication carbonApp,
                                  AxisConfiguration axisConfig) {
        log.info("Undeploying Carbon Application : " + carbonApp.getAppNameWithVersion() + "...");
        // Call the undeployer handler chain
        try {
            for (AppDeploymentHandler handler : appDeploymentHandlers) {
                handler.undeployArtifacts(carbonApp, axisConfig);
            }
            // Remove the app from tenant cApp list
            removeCarbonApp(AppDeployerUtils.getTenantIdString(), carbonApp);

            // Remove the app from registry
            // removing the extracted CApp form tmp/carbonapps/
            FileManipulator.deleteDir(carbonApp.getExtractedPath());
            log.info("Successfully Undeployed Carbon Application : " + carbonApp.getAppNameWithVersion()
                             + AppDeployerUtils.getTenantIdLogString(AppDeployerUtils.getTenantId()));
        } catch (Exception e) {
            log.error("Error occured while trying unDeply  : " + carbonApp.getAppNameWithVersion());
        }

    }

    /**
     * Remove a cApp for a particular tenant
     *
     * @param tenantId - tenant id of the cApp
     * @param carbonApp - CarbonApplication instance
     */
    public void removeCarbonApp(String tenantId, CarbonApplication carbonApp) {
        ArrayList<CarbonApplication> cApps = tenantcAppMap.get(tenantId);
        synchronized (cApps) {
            if (cApps != null && cApps.contains(carbonApp)) {
                cApps.remove(carbonApp);
            }
        }
    }

    /**
     *  Get the list of faulty CarbonApplications for the give tenant id. If the list is null,
     *  return an empty Arraylist
     *
     * @param tenantId - tenant id to find faulty cApps
     * @return - list of tenant faulty cApps
     */
    public HashMap<String, Exception> getFaultyCarbonApps(String tenantId) {
        HashMap<String, Exception> cApps = tenantfaultycAppMap.get(tenantId);
        if (cApps == null) {
            cApps = new HashMap<String, Exception>();
        }
        return cApps;
    }

    /**
     * Remove a faulty cApp for a particular tenant
     *
     * @param tenantId
     * @param appFilePath
     */
    public void removeFaultyCarbonApp(String tenantId, String appFilePath) {
        HashMap<String, Exception> faultycApps = tenantfaultycAppMap.get(tenantId);
        synchronized (faultycApps) {
            if (faultycApps != null) {
                faultycApps.remove(appFilePath);
            }
        }
    }

}
