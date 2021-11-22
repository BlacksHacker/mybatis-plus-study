### MyBatis-plus学习总结


#### 1. 简介

   Mybatis-Plus 是MyBatis的增强工具，在MyBatis上只做增强，不做修改，为简化开发和提高效率而生。它自身封装好了一些crud方法，不需要我们再次编写具体的xml.[MyBatis官网地址](https://mp.baomidou.com/#/)

#### 2. Spring整合MyBatis-Plus
   
   
   
### MyBatis源码学习

#### 架构原理

##### 架构图


1. MyBatis配置文件
    
    - SqlMapConfig.xml 作为mybatis的全局配置文件，配置mybatis的运行环境
    - Mapper.xml mybatis的sql映射文件，需要在SqlMapConfig.xml中加载
    
2. SqlSessionFactory
    
    - 通过Mybatis配置文件构建会话工厂SqlSessionFactory
    
3. SqlSession 

    - 通过会话工厂创建的sqlSession即会话，通过sqlSession进行数据库的操作
    
4. Executor执行器

    - Mybatis运行的核心，调度器，调用mybatis三大组件的执行。
    
5. MappedStatement

    - Mybatis底层封装对象，包含mybatis配置信息和sql的映射信息，mapper.xml中一个delete标签对应一个Mapped Statement对象，标签的ID即是MappedStatement的id。
    - MappedStatement对sql执行输入参数定义，包括HashMap、基本类型和pojo，Executor通过MappedStatement在执行sql前将输入的java对象映射至sql中，输入参数映射就是jdbc编程中对preparedStatement设置参数
    - MappedStatement对sql执行输出结果定义，输出结果映射过程相当于jdbc编程中对结果的解析处理过程。
6. 


#### SqlSessionFactory的创建

初始化Mybatis的过程，就是创建SqlSessionFactory单例的过程
1. mybatis读取全局xml配置文件，解析xml元素结点
2. 将xml结点值设置到configuration实例的相关变量中
3. 由configuration实例创建SqlSessionFactory实例

核心类

| 类名   | 作用   |
| ---- | ---- |
|  SqlSessionFactoryBuilder    | 用来创建SqlSessionFactory实例。链式创建模式     |
| XMLConfigBuilder| 1.解析XML文件，生成XNode对象</br> 2.创建并初始化Configuration实例。3.解析XNode的键值对，转换为属性设置到configuration实例中|
| Configuration| 数据类，包含mybatis所有配置信息，初始化最重要的内容就是创建并初始化该实例|
|SqlSessionFactory| 创建sqlSession实例的工厂类，一般当作单例使用，默认实现DefaultSessionFactory|

流程

```java
public class SqlSessionFactoryBuilder {
     public SqlSessionFactory build(InputStream inputStream, String environment, Properties properties) {
        try {
            //inputStream XML输入流
            //environment sqlSessionFactory的数据库环境
            //properties 动态常量
          XMLConfigBuilder parser = new XMLConfigBuilder(inputStream, environment, properties);
          //构建configuration实例对象
          return build(parser.parse());
        } catch (Exception e) {
          throw ExceptionFactory.wrapException("Error building SqlSession.", e);
        } finally {
          ErrorContext.instance().reset();
          try {
            inputStream.close();
          } catch (IOException e) {
            // Intentionally ignore. Prefer previous error.
          }
        }
     }    
    
     public SqlSessionFactory build(Configuration config) {
       return new DefaultSqlSessionFactory(config);
     } 
}
```

```java
public class XMLConfigBuilder extends BaseBuilder {
    public XMLConfigBuilder(InputStream inputStream, String environment, Properties props) {
        //利用输入流生成XPathParser解析对象
        this(new XPathParser(inputStream, true, props, new XMLMapperEntityResolver()), environment, props);
      }
    
    private XMLConfigBuilder(XPathParser parser, String environment, Properties props) {
        super(new Configuration());
        ErrorContext.instance().resource("SQL Mapper Configuration");
        this.configuration.setVariables(props);
        this.parsed = false;
        this.environment = environment;
        this.parser = parser;
    }
    
    public Configuration parse() {
        if (parsed) {
          throw new BuilderException("Each XMLConfigBuilder can only be used once.");
        }
        parsed = true;
        parseConfiguration(parser.evalNode("/configuration"));
        return configuration;
    }
    
    private void parseConfiguration(XNode root) {
        try {
          Properties settings = settingsAsPropertiess(root.evalNode("settings"));
          //issue #117 read properties first
          propertiesElement(root.evalNode("properties"));
          loadCustomVfs(settings);
          typeAliasesElement(root.evalNode("typeAliases"));
          pluginElement(root.evalNode("plugins"));
          objectFactoryElement(root.evalNode("objectFactory"));
          objectWrapperFactoryElement(root.evalNode("objectWrapperFactory"));
          reflectionFactoryElement(root.evalNode("reflectionFactory"));
          settingsElement(settings);
          // read it after objectFactory and objectWrapperFactory issue #631
          environmentsElement(root.evalNode("environments"));
          databaseIdProviderElement(root.evalNode("databaseIdProvider"));
          typeHandlerElement(root.evalNode("typeHandlers"));
          mapperElement(root.evalNode("mappers"));
        } catch (Exception e) {
          throw new BuilderException("Error parsing SQL Mapper Configuration. Cause: " + e, e);
        }
    }
}
```