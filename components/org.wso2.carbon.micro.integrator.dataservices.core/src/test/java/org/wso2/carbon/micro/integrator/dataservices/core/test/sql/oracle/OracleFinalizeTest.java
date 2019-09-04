package org.wso2.carbon.micro.integrator.dataservices.core.test.sql.oracle;

import org.wso2.carbon.micro.integrator.dataservices.core.test.DataServiceBaseTestCase;
import org.wso2.carbon.micro.integrator.dataservices.core.test.util.UtilServer;

public class OracleFinalizeTest extends DataServiceBaseTestCase {

	public OracleFinalizeTest() {
		super("OracleFinalizeTest");
	}
	
	public void testMySQLStop() throws Exception {
		UtilServer.stop();
               endTenantFlow();
	}

}
