package com.eeeffff.yapidoc.utils;

import static com.eeeffff.yapidoc.constants.SpringMvcConstants.CONTROLLER_SET;

import java.util.List;

import com.thoughtworks.qdox.model.JavaAnnotation;
import com.thoughtworks.qdox.model.JavaClass;

/**
 * 
 * @version V1.0
 * @date 2019-07-20 15:09
 */
public final class SpringMvcUtils {
	private SpringMvcUtils() {

	}

	/**
	 * 是否为controller
	 *
	 * @param cls cls
	 * @return true or false
	 */
	public static boolean isController(JavaClass cls) {
		List<JavaAnnotation> classAnnotations = cls.getAnnotations();
		for (JavaAnnotation annotation : classAnnotations) {
			String annotationName = annotation.getType().getName();
			if (CONTROLLER_SET.contains(annotationName)) {
				return true;
			}
		}
		return false;
	}
}