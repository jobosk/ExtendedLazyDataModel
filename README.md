
# Objective:

Extend the LazyDataModel by Primefaces in order to implement sorting, pagination and filtering for the entity contained.

# Explanation:

With the 'JPAMetaModelEntityProcessor' we automatically generate the JPA metamodel of the application classes, which we will need afterwards for type safety when interpreting the table filters, in order to build the criteria for the JPA repository request. Primefaces will automatically load the extended table model and execute the overriden load method, triggering the process that builds the JPA specification for the request, the sorting of the filters applied, and the pagination specified.

# Dependencies: 

## Back-end:

`<dependency>`  
`<groupId>org.hibernate.javax.persistence</groupId>`  
`<artifactId>hibernate-jpa-2.1-api</artifactId>`  
`<version>1.0.0.Final</version>`  
`</dependency>`  

`<dependency>`  
`<groupId>org.springframework.data</groupId>`  
`<artifactId>spring-data-jpa</artifactId>`  
`<version>${springdata.version}</version>`  
`</dependency>`  

## Front-end:

`<dependency>`  
`    <groupId>org.primefaces</groupId>`  
`    <artifactId>primefaces</artifactId>`  
`    <version>6.0</version>`  
`</dependency>`  

## Custom:

`<dependency>`  
`  <groupId>org.apache.commons</groupId>`  
`  <artifactId>commons-lang3</artifactId>`  
`  <version>3.0</version>`  
`</dependency>`  

# Plugins (back-end):

`<plugin>`  
`<groupId>org.bsc.maven</groupId>`  
`<artifactId>maven-processor-plugin</artifactId>`  
`<version>2.0.5</version>`  
`<dependencies>`  
`<dependency>`  
`<groupId>org.hibernate</groupId>`  
`<artifactId>hibernate-jpamodelgen</artifactId>`  
`<version>1.2.0.Final</version>`  
`</dependency>`  
`</dependencies>`  
`<executions>`  
`<execution>`  
`<id>process</id>`  
`<goals>`  
`<goal>process</goal>`  
`</goals>`  
`<phase>generate-sources</phase>`  
`<configuration>`  
`<outputDirectory>target/metamodel</outputDirectory>`  
`<processors>`  
`<processor>org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor</processor>`  
`</processors>`  
`</configuration>`  
`</execution>`  
`</executions>`  
`</plugin>`  

`<plugin>`  
`<groupId>org.codehaus.mojo</groupId>`  
`<artifactId>build-helper-maven-plugin</artifactId>`  
`<executions>`  
`<execution>`  
`<id>add-source</id>`  
`<phase>generate-sources</phase>`  
`<goals>`  
`<goal>add-source</goal>`  
`</goals>`  
`<configuration>`  
`<sources>`  
`<source>target/metamodel</source>`  
`</sources>`  
`</configuration>`  
`</execution>`  
`</executions>`  
`</plugin>`  

# Usage:

## In the JSF managed bean (FooBean):

private LazyDataModel<Foo> model = new ExtendedLazyDataModel<Foo>(fooRepository);  

## In the table layout (foo.xhtml):

`<p:dataTable ... value="#{fooBean.model}" paginator="true" lazy="true" ... >`  
...  
`<p:column filterBy="#{foo.attribute}" filterMatchMode="contains" sortBy="#{foo.attribute}">`  
...  
`</p:column>`  
...  
`</p:dataTable>`
