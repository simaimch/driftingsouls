package net.driftingsouls.ds2.server.framework.pipeline.generators;

import java.lang.annotation.*;

/**
 * Ein URL-Parameter.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD,ElementType.TYPE,ElementType.PARAMETER})
public @interface UrlParam {
	/**
	 * Der Name des Parameters.
	 */
	String name() default "";

	/**
	 * Der Parametertyp.
	 */
	UrlParamType type() default UrlParamType.STRING;

	/**
	 * Die Beschreibung des Parameters.
	 */
	String description() default "";
}
