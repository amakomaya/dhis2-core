/*
 * Copyright 2012-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.hisp.dhis.web.tomcat;

import java.util.Map;
import javax.servlet.MultipartConfigElement;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.util.StringUtils;

/**
 * Handler for {@link WebServlet @WebServlet}-annotated classes.
 *
 * @author Andy Wilkinson
 */
class WebServletHandler extends ServletComponentHandler {

	WebServletHandler() {
		super(WebServlet.class);
	}

	@Override
	public void doHandle(Map<String, Object> attributes, AnnotatedBeanDefinition beanDefinition,
			BeanDefinitionRegistry registry) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(ServletRegistrationBean.class);
		builder.addPropertyValue("asyncSupported", attributes.get("asyncSupported"));
		builder.addPropertyValue("initParameters", extractInitParameters(attributes));
		builder.addPropertyValue("loadOnStartup", attributes.get("loadOnStartup"));
		String name = determineName(attributes, beanDefinition);
		builder.addPropertyValue("name", name);
		builder.addPropertyValue("servlet", beanDefinition);
		builder.addPropertyValue("urlMappings", extractUrlPatterns(attributes));
		builder.addPropertyValue("multipartConfig", determineMultipartConfig(beanDefinition));
		registry.registerBeanDefinition(name, builder.getBeanDefinition());
	}

	private String determineName(Map<String, Object> attributes, BeanDefinition beanDefinition) {
		return (String) (StringUtils.hasText((String) attributes.get("name")) ? attributes.get("name")
				: beanDefinition.getBeanClassName());
	}

	private BeanDefinition determineMultipartConfig(AnnotatedBeanDefinition beanDefinition) {
		Map<String, Object> attributes = beanDefinition.getMetadata()
			.getAnnotationAttributes(MultipartConfig.class.getName());
		if (attributes == null) {
			return null;
		}
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(MultipartConfigElement.class);
		builder.addConstructorArgValue(attributes.get("location"));
		builder.addConstructorArgValue(attributes.get("maxFileSize"));
		builder.addConstructorArgValue(attributes.get("maxRequestSize"));
		builder.addConstructorArgValue(attributes.get("fileSizeThreshold"));
		return builder.getBeanDefinition();
	}

}
