package org.wso2.carbon.micro.integrator.dataservices.core.test.sql.oracle;

import org.wso2.carbon.micro.integrator.dataservices.core.test.sql.AbstractBasicServiceTest;

public class OracleBasicTest extends AbstractBasicServiceTest {

	public OracleBasicTest(String testName) {
		super(testName, "OracleBasicService");
	}
	
	public void testOracleBasicSelectWithFields() {
		this.basicSelectWithFields();
	}

	public void testOracleBasicSelectAll() {
		this.basicSelectAll();
	}
	
	public void testOracleBasicSelectCount() {
		this.basicSelectCount();
	}
	
	public void testOracleBasicSelectWithAttributesForDateTime() {
		this.basicSelectWithAttributesForDateTime();
	}
	
	public void testOracleBasicArrayInputTypes() {
		this.basicArrayInputTypes();
	}

}
