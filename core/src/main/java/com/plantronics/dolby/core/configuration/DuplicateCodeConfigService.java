package com.plantronics.dolby.core.configuration;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;


@Component(immediate = true,  service = DuplicateCodeConfigService.class,configurationPid = "com.plantronics.dolby.core.configuration.DuplicateCodeConfigService")
@Designate(ocd = DuplicateCodeConfigService.DupcodeConfig.class)
public class DuplicateCodeConfigService {

	/**
	 * Implementation of the DupcodeConfig
	 */
	@ObjectClassDefinition(name = "Plantronics Duplicate code configuration settings")
	public @interface DupcodeConfig {

		public static final String DUPCODES_FILE = "dupcodes";
		public static final String NEWCODES_FILE = "newcodes";
		public static final String BASE_PATH = "basepath";
		public static final String ACTIVATION_CODES_NODE = "activationcodespath";

		@AttributeDefinition(name = DUPCODES_FILE, description = "Duplicate codes file")
		String dupcodesFile() default "/content/dam/plantronics/dupcodes.csv";

		@AttributeDefinition(name = NEWCODES_FILE, description = "New codes file")
		String newcodesFile() default "/content/dam/plantronics/newcodes.csv";

		@AttributeDefinition(name = BASE_PATH, description = "Location Status Path")
		String basePath() default "/content/dolbyatmos/";

		@AttributeDefinition(name = ACTIVATION_CODES_NODE, description = "Activation codes name ")
		String activationcodesNode() default "activationcodes";

	}

	private DupcodeConfig config;
	public String getDupcodesFile() {
		return config.dupcodesFile();
	}

	public String getNewcodesFile() {
		return config.newcodesFile();
	}

	public String getBasepath() {
		return config.basePath();
	}

	public String getActivationcodesNode() {
		return config.activationcodesNode();
	}

	@Activate
	@Modified
	protected void activate(DupcodeConfig config) {
		this.config = config;
	}
	
	   @Deactivate
	    protected void deactivate() {
	    }
}
