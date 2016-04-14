/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.web;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.data.web.JsonPath;
import org.springframework.data.web.JsonProjectingMethodInterceptorFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import com.jayway.jsonpath.spi.mapper.MappingProvider;

/**
 * @author Oliver Gierke
 */
public class JsonProjectingMethodInterceptorFactoryUnitTests {

	ProjectionFactory projectionFactory;
	Customer customer;

	@Before
	public void setUp() {

		String json = "{\"firstname\" : \"Dave\", "//
				+ "\"address\" : { \"zipCode\" : \"01097\", \"city\" : \"Dresden\" }," //
				+ "\"addresses\" : [ { \"zipCode\" : \"01097\", \"city\" : \"Dresden\" }]" + " }";

		SpelAwareProxyProjectionFactory projectionFactory = new SpelAwareProxyProjectionFactory();

		MappingProvider mappingProvider = new JacksonMappingProvider(new ObjectMapper());
		projectionFactory.registerMethodInvokerFactory(new JsonProjectingMethodInterceptorFactory(mappingProvider));

		this.projectionFactory = projectionFactory;
		this.customer = projectionFactory.createProjection(Customer.class, new ByteArrayInputStream(json.getBytes()));
	}

	@Test
	public void accessSimpleProperty() {
		assertThat(customer.getFirstname(), is("Dave"));
	}

	@Test
	public void accessPropertyWithExplicitAnnotation() {
		assertThat(customer.getBar(), is("Dave"));
	}

	@Test
	public void accessPropertyWithComplexReturnType() {
		assertThat(customer.getAddress(), is(new Address("01097", "Dresden")));
	}

	@Test
	public void accessComplexPropertyWithProjection() {
		assertThat(customer.getAddressProjection().getCity(), is("Dresden"));
	}

	@Test
	public void accessPropertyWithNestedJsonPath() {
		assertThat(customer.getNestedZipCode(), is("01097"));

	}

	@Test
	public void accessCollectionProperty() {
		assertThat(customer.getAddresses().get(0), is(new Address("01097", "Dresden")));
	}

	@Test
	public void accessPropertyOnNestedProjection() {
		assertThat(customer.getAddressProjections().get(0).getZipCode(), is("01097"));
	}

	@Test
	public void accessPropertyThatUsesJsonPathProjectionInTurn() {
		assertThat(customer.getAnotherAddressProjection().getZipCodeButNotCity(), is("01097"));
	}

	@Test
	public void accessCollectionPropertyThatUsesJsonPathProjectionInTurn() {

		List<AnotherAddressProjection> projections = customer.getAnotherAddressProjections();

		assertThat(projections, hasSize(1));
		assertThat(projections.get(0).getZipCodeButNotCity(), is("01097"));
	}

	@Test
	public void accessAsCollectionPropertyThatUsesJsonPathProjectionInTurn() {

		Set<AnotherAddressProjection> projections = customer.getAnotherAddressProjectionAsCollection();

		assertThat(projections, hasSize(1));
		assertThat(projections.iterator().next().getZipCodeButNotCity(), is("01097"));
	}

	@Test
	public void accessNestedPropertyButStayOnRootLevel() {

		Name name = customer.getName();

		assertThat(name, is(notNullValue()));
		assertThat(name.getFirstname(), is("Dave"));
	}

	interface Customer {

		String getFirstname();

		@JsonPath("$")
		Name getName();

		Address getAddress();

		List<Address> getAddresses();

		@JsonPath("$.addresses")
		List<AddressProjection> getAddressProjections();

		@JsonPath("$.firstname")
		String getBar();

		@JsonPath("$.address")
		AddressProjection getAddressProjection();

		@JsonPath("$.address.zipCode")
		String getNestedZipCode();

		@JsonPath("$.address")
		AnotherAddressProjection getAnotherAddressProjection();

		@JsonPath("$.addresses")
		List<AnotherAddressProjection> getAnotherAddressProjections();

		@JsonPath("$.address")
		Set<AnotherAddressProjection> getAnotherAddressProjectionAsCollection();
	}

	interface AddressProjection {

		String getZipCode();

		String getCity();
	}

	interface Name {

		@JsonPath("$.firstname")
		String getFirstname();

		@JsonPath("$.lastname")
		String getLastname();
	}

	interface AnotherAddressProjection {

		@JsonPath("$.zipCode")
		String getZipCodeButNotCity();
	}

	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	static class Address {
		private String zipCode, city;
	}
}