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


#### 1.SqlSessionFactory的创建

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
            //决定mybatis的运行方式，如CacheEnabled LazyLoadingEnabled等属性
          Properties settings = settingsAsPropertiess(root.evalNode("settings"));
          //issue #117 read properties first
          //用来配置变量动态化
            //datasource的username password等等
          propertiesElement(root.evalNode("properties"));
          loadCustomVfs(settings);
            //定义别名，为java全路径定义短名称
          typeAliasesElement(root.evalNode("typeAliases"));
          // 定义插件，用来拦截某些类，如：Executor， ParameterHandler, StatementHandler, ResultSetHandler
          pluginElement(root.evalNode("plugins"));
          objectFactoryElement(root.evalNode("objectFactory"));
          objectWrapperFactoryElement(root.evalNode("objectWrapperFactory"));
          reflectionFactoryElement(root.evalNode("reflectionFactory"));
          settingsElement(settings);
          // read it after objectFactory and objectWrapperFactory issue #631
          //定义数据库环境，可以配置多个，每个datasource对应一个transactionManager
          environmentsElement(root.evalNode("environments"));
          //定义数据库厂商标识，根据不同厂商执行不同sql语句
          databaseIdProviderElement(root.evalNode("databaseIdProvider"));
          //定义类型处理器，用来将数据库中获取的值转为Java类型
          typeHandlerElement(root.evalNode("typeHandlers"));
          //解析mappers 节点 映射器，也就是sql映射语句
          mapperElement(root.evalNode("mappers"));
        } catch (Exception e) {
          throw new BuilderException("Error parsing SQL Mapper Configuration. Cause: " + e, e);
        }
    }
}
```

###### mapperElement

mappers定义了SQL语句映射关系

```java
public class XMLConfigBuilder extends BaseBuilder {
  private void mapperElement(XNode parent) throws Exception {
    if (parent != null) {
      for (XNode child : parent.getChildren()) {
        //子元素为package时将其下所有接口认为时mapper类，创建其类对象，加入到mapperRegistry中
        if ("package".equals(child.getName())) {
          String mapperPackage = child.getStringAttribute("name");
          configuration.addMappers(mapperPackage);
        } else {
          String resource = child.getStringAttribute("resource");
          String url = child.getStringAttribute("url");
          String mapperClass = child.getStringAttribute("class");
          if (resource != null && url == null && mapperClass == null) {
            //resource不为空时，加载对应的XML资源，并进行解析
            ErrorContext.instance().resource(resource);
            InputStream inputStream = Resources.getResourceAsStream(resource);
            XMLMapperBuilder mapperParser = new XMLMapperBuilder(inputStream, configuration, resource, configuration.getSqlFragments());
            mapperParser.parse();
          } else if (resource == null && url != null && mapperClass == null) {
              //url不为空时，加载对应的XML资源，并进行解析
            ErrorContext.instance().resource(url);
            InputStream inputStream = Resources.getUrlAsStream(url);
            XMLMapperBuilder mapperParser = new XMLMapperBuilder(inputStream, configuration, url, configuration.getSqlFragments());
            mapperParser.parse();
          } else if (resource == null && url == null && mapperClass != null) {
            Class<?> mapperInterface = Resources.classForName(mapperClass);
            configuration.addMapper(mapperInterface);
          } else {
            throw new BuilderException("A mapper element may only specify a url, resource or class, but not more than one.");
          }
        }
      }
    }
  }
}
```
采用代理模式，调用了MapperRegistry的addMappers方法

```java
public class XMLConfigBuilder extends BaseBuilder {
    
      public void addMappers(String packageName) {
        mapperRegistry.addMappers(packageName);
      }    
}

public class MapperRegistry {
      public void addMappers(String packageName) {
        addMappers(packageName, Object.class);
      }
        
       public void addMappers(String packageName, Class<?> superType) {
         ResolverUtil<Class<?>> resolverUtil = new ResolverUtil<Class<?>>();
         resolverUtil.find(new ResolverUtil.IsA(superType), packageName);
         Set<Class<? extends Class<?>>> mapperSet = resolverUtil.getClasses();
         for (Class<?> mapperClass : mapperSet) {
           addMapper(mapperClass);
         }
       }
    
      public <T> void addMapper(Class<T> type) {
        if (type.isInterface()) {
          if (hasMapper(type)) {
            throw new BindingException("Type " + type + " is already known to the MapperRegistry.");
          }
          boolean loadCompleted = false;
          try {
            //将新解析的mapper接口类添加到map中
            knownMappers.put(type, new MapperProxyFactory<T>(type));
            // It's important that the type is added before the parser is run
            // otherwise the binding may automatically be attempted by the
            // mapper parser. If the type is already known, it won't try.
            //解析mapper接口的各项注解，比如@Select等
            MapperAnnotationBuilder parser = new MapperAnnotationBuilder(config, type);
            parser.parse();
            loadCompleted = true;
          } finally {
            if (!loadCompleted) {
              knownMappers.remove(type);
            }
          }
        }
      }

}
```
knowsMappers是是一个HashMap类型, 其键值就是mapper接口的class对象，值为根据mapper接口的class对象为参数创建的MapperProxyFactory工厂类，
后续通过MapperRegistry获取mapper时，是通过其对应的mapper代理工厂MapperProxyFactory，获取mapper的代理对象。MapperProxy本质是一个代理接口InvocationHandler的实现类
```java
 private final Map<Class<?>, MapperProxyFactory<?>> knownMappers = new HashMap<Class<?>, MapperProxyFactory<?>>();

 public class MapperProxyFactory<T> {
 
   private final Class<T> mapperInterface;
   private final Map<Method, MapperMethod> methodCache = new ConcurrentHashMap<Method, MapperMethod>();
 
   public MapperProxyFactory(Class<T> mapperInterface) {
     this.mapperInterface = mapperInterface;
   }
 
   public Class<T> getMapperInterface() {
     return mapperInterface;
   }
 
   public Map<Method, MapperMethod> getMethodCache() {
     return methodCache;
   }
 
   @SuppressWarnings("unchecked")
   protected T newInstance(MapperProxy<T> mapperProxy) {
     return (T) Proxy.newProxyInstance(mapperInterface.getClassLoader(), new Class[] { mapperInterface }, mapperProxy);
   }
    
   //获取mapper本质是获取mapper的代理对象 mapperProxy只是增强接口的实现类
   public T newInstance(SqlSession sqlSession) {
     final MapperProxy<T> mapperProxy = new MapperProxy<T>(sqlSession, mapperInterface, methodCache);
     return newInstance(mapperProxy);
   }
 
 }

 public class MapperProxy<T> implements InvocationHandler, Serializable {
 
   private static final long serialVersionUID = -6424540398559729838L;
   private final SqlSession sqlSession;
   private final Class<T> mapperInterface;
   private final Map<Method, MapperMethod> methodCache;
 
   public MapperProxy(SqlSession sqlSession, Class<T> mapperInterface, Map<Method, MapperMethod> methodCache) {
     this.sqlSession = sqlSession;
     this.mapperInterface = mapperInterface;
     this.methodCache = methodCache;
   }
 
    //代理方法
   @Override
   public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
     if (Object.class.equals(method.getDeclaringClass())) {
       try {
         return method.invoke(this, args);
       } catch (Throwable t) {
         throw ExceptionUtil.unwrapThrowable(t);
       }
     }
     final MapperMethod mapperMethod = cachedMapperMethod(method);
     return mapperMethod.execute(sqlSession, args);
   }
 
   private MapperMethod cachedMapperMethod(Method method) {
     MapperMethod mapperMethod = methodCache.get(method);
     if (mapperMethod == null) {
       mapperMethod = new MapperMethod(mapperInterface, method, sqlSession.getConfiguration());
       methodCache.put(method, mapperMethod);
     }
     return mapperMethod;
   }
 
 }
```
与此同时在MapperRegistry的addMapper中，根据不同的mapper 的class创建对应的MapperAnnotationBuilder，通过parse解析，以及parseStatement方法的调用，将mapper中的对应的具体方法method解析，生成MapperStatement,即一个mapper类的一个方法对应一个MapperStatement,并将MapperStatement
加入到Configuration的对象的mappedStatements属性中，其结构为StrictMap，以mapperStatement的id为key，mapperStatement为value。其ID本质为mapper的类名+方法名，构成mapperStatement的唯一ID.
```java
public class MapperRegistry {
     public <T> void addMapper(Class<T> type) {
            if (type.isInterface()) {
              if (hasMapper(type)) {
                throw new BindingException("Type " + type + " is already known to the MapperRegistry.");
              }
              boolean loadCompleted = false;
              try {
                //将新解析的mapper接口类添加到map中
                knownMappers.put(type, new MapperProxyFactory<T>(type));
                // It's important that the type is added before the parser is run
                // otherwise the binding may automatically be attempted by the
                // mapper parser. If the type is already known, it won't try.
                //解析mapper接口的各项注解，比如@Select等,根据不同的mapper创建不同的MapperAnnotationBuilder
                MapperAnnotationBuilder parser = new MapperAnnotationBuilder(config, type);
                parser.parse();
                loadCompleted = true;
              } finally {
                if (!loadCompleted) {
                  knownMappers.remove(type);
                }
              }
            }
          }    
}

public class MapperAnnotationBuilder{
  public void parse() {
    String resource = type.toString();
    if (!configuration.isResourceLoaded(resource)) {
      loadXmlResource();
      configuration.addLoadedResource(resource);
      assistant.setCurrentNamespace(type.getName());
      parseCache();
      parseCacheRef();
      Method[] methods = type.getMethods();
      for (Method method : methods) {
        try {
          // issue #237
          if (!method.isBridge()) {
            //循环解析method
            parseStatement(method);
          }
        } catch (IncompleteElementException e) {
          configuration.addIncompleteMethod(new MethodResolver(this, method));
        }
      }
    }
    parsePendingMethods();
  }
    
  void parseStatement(Method method) {
      Class<?> parameterTypeClass = getParameterType(method);
      LanguageDriver languageDriver = getLanguageDriver(method);

        //sqlSource是MapperStatement的一个属性，用来提供BoundSQL对象
        //BoundSql用来建立sql语句，它包含sql String，入参parameterObject和入参映射parameterMappings。它利用sql语句和入参，组装成最终的访问数据库的SQL语句，包括动态SQL。这是mybatis Mapper映射的最核心的地方
      SqlSource sqlSource = getSqlSourceFromAnnotations(method, parameterTypeClass, languageDriver);
      if (sqlSource != null) {
        Options options = method.getAnnotation(Options.class);
        // 根据mapper的名称加上method的名称构成唯一id
        final String mappedStatementId = type.getName() + "." + method.getName();
        Integer fetchSize = null;
        Integer timeout = null;
        StatementType statementType = StatementType.PREPARED;
        ResultSetType resultSetType = ResultSetType.FORWARD_ONLY;
        SqlCommandType sqlCommandType = getSqlCommandType(method);
        boolean isSelect = sqlCommandType == SqlCommandType.SELECT;
        boolean flushCache = !isSelect;
        boolean useCache = isSelect;
  
        KeyGenerator keyGenerator;
        String keyProperty = "id";
        String keyColumn = null;
        if (SqlCommandType.INSERT.equals(sqlCommandType) || SqlCommandType.UPDATE.equals(sqlCommandType)) {
          // first check for SelectKey annotation - that overrides everything else
          SelectKey selectKey = method.getAnnotation(SelectKey.class);
          if (selectKey != null) {
            keyGenerator = handleSelectKeyAnnotation(selectKey, mappedStatementId, getParameterType(method), languageDriver);
            keyProperty = selectKey.keyProperty();
          } else if (options == null) {
            keyGenerator = configuration.isUseGeneratedKeys() ? new Jdbc3KeyGenerator() : new NoKeyGenerator();
          } else {
            keyGenerator = options.useGeneratedKeys() ? new Jdbc3KeyGenerator() : new NoKeyGenerator();
            keyProperty = options.keyProperty();
            keyColumn = options.keyColumn();
          }
        } else {
          keyGenerator = new NoKeyGenerator();
        }
  
        if (options != null) {
          flushCache = options.flushCache();
          useCache = options.useCache();
          fetchSize = options.fetchSize() > -1 || options.fetchSize() == Integer.MIN_VALUE ? options.fetchSize() : null; //issue #348
          timeout = options.timeout() > -1 ? options.timeout() : null;
          statementType = options.statementType();
          resultSetType = options.resultSetType();
        }
  
        String resultMapId = null;
        ResultMap resultMapAnnotation = method.getAnnotation(ResultMap.class);
        if (resultMapAnnotation != null) {
          String[] resultMaps = resultMapAnnotation.value();
          StringBuilder sb = new StringBuilder();
          for (String resultMap : resultMaps) {
            if (sb.length() > 0) {
              sb.append(",");
            }
            sb.append(resultMap);
          }
          resultMapId = sb.toString();
        } else if (isSelect) {
          resultMapId = parseResultMap(method);
        }
        //通过代理模式，内部本质是configuration进行添加
        assistant.addMappedStatement(
            mappedStatementId,
            sqlSource,
            statementType,
            sqlCommandType,
            fetchSize,
            timeout,
            // ParameterMapID
            null,
            parameterTypeClass,
            resultMapId,
            getReturnType(method),
            resultSetType,
            flushCache,
            useCache,
            // TODO issue #577
            false,
            keyGenerator,
            keyProperty,
            keyColumn,
            // DatabaseID
            null,
            languageDriver,
            // ResultSets
            null);
      }
    }
}

public class MapperBuildAssistant{
     public MappedStatement addMappedStatement(
          String id,
          SqlSource sqlSource,
          StatementType statementType,
          SqlCommandType sqlCommandType,
          Integer fetchSize,
          Integer timeout,
          String parameterMap,
          Class<?> parameterType,
          String resultMap,
          Class<?> resultType,
          ResultSetType resultSetType,
          boolean flushCache,
          boolean useCache,
          boolean resultOrdered,
          KeyGenerator keyGenerator,
          String keyProperty,
          String keyColumn,
          String databaseId,
          LanguageDriver lang,
          String resultSets) {
    
        if (unresolvedCacheRef) {
          throw new IncompleteElementException("Cache-ref not yet resolved");
        }
    
        id = applyCurrentNamespace(id, false);
        boolean isSelect = sqlCommandType == SqlCommandType.SELECT;
    
        MappedStatement.Builder statementBuilder = new MappedStatement.Builder(configuration, id, sqlSource, sqlCommandType)
            .resource(resource)
            .fetchSize(fetchSize)
            .timeout(timeout)
            .statementType(statementType)
            .keyGenerator(keyGenerator)
            .keyProperty(keyProperty)
            .keyColumn(keyColumn)
            .databaseId(databaseId)
            .lang(lang)
            .resultOrdered(resultOrdered)
            .resulSets(resultSets)
            .resultMaps(getStatementResultMaps(resultMap, resultType, id))
            .resultSetType(resultSetType)
            .flushCacheRequired(valueOrDefault(flushCache, !isSelect))
            .useCache(valueOrDefault(useCache, isSelect))
            .cache(currentCache);
    
        ParameterMap statementParameterMap = getStatementParameterMap(parameterMap, parameterType, id);
        if (statementParameterMap != null) {
          statementBuilder.parameterMap(statementParameterMap);
        }   
        //创建mapperStatement，调用configuration方法，加入MapperStatements中
        MappedStatement statement = statementBuilder.build();
        configuration.addMappedStatement(statement);
        return statement; 
    
    }
}

public class Configuration{
    protected final Map<String, MappedStatement> mappedStatements = new StrictMap<MappedStatement>("Mapped Statements collection");

  public void addMappedStatement(MappedStatement ms) {
    //以ms的id为key，ms为Value
    mappedStatements.put(ms.getId(), ms);
  }
}
```
在最后XMLConfigBuilder完成解析配置后，生成Configuration对象实例，生成SqlSessionFactory的默认实现类DefaultSqlSessionFactory的实例返回
```java
public class SqlSessionFactoryBuilder{
  public SqlSessionFactory build(Configuration config) {
    return new DefaultSqlSessionFactory(config);
  }
}
```

至此 mybatis的初始化完毕！

##### 总结

##### 贴图

#### 2.SqlSession的创建

   Mybatis运行阶段，每次运行都是通过SqlSession来执行，是作为运行的核心，其本身不是线程安全的，一般放在局部作用域中，用完close掉。
 
 以SqlSessionFactory的默认实现类DefaultSqlSessionFactory为例
 ```java
public class DefaultSqlSessionFactory implements SqlSessionFactory {
      @Override
      public SqlSession openSession() {
        return openSessionFromDataSource(configuration.getDefaultExecutorType(), null, false);
      }
    
      private SqlSession openSessionFromDataSource(ExecutorType execType, TransactionIsolationLevel level, boolean autoCommit) {
          Transaction tx = null;
          try {
            // 获取environment配置
            final Environment environment = configuration.getEnvironment();
            //通过environment配置构造事务工厂TransactionFactory
            final TransactionFactory transactionFactory = getTransactionFactoryFromEnvironment(environment);
            tx = transactionFactory.newTransaction(environment.getDataSource(), level, autoCommit);
            //根据事务和配置的Execute类型构造执行器Executor
            final Executor executor = configuration.newExecutor(tx, execType);
            //创建SqlSession的默认实现类DefaultSqlSession的实例对象
            return new DefaultSqlSession(configuration, executor, autoCommit);
          } catch (Exception e) {
            closeTransaction(tx); // may have fetched a connection so lets call close()
            throw ExceptionFactory.wrapException("Error opening session.  Cause: " + e, e);
          } finally {
            ErrorContext.instance().reset();
          }
        }

      private TransactionFactory getTransactionFactoryFromEnvironment(Environment environment) {
        // 环境配置中没有设定事务配置时，默认采用容器进行事务管理
         if (environment == null || environment.getTransactionFactory() == null) {
           return new ManagedTransactionFactory();
         }
         return environment.getTransactionFactory();
       }
}
```
sqlSession的创建本质是其默认实现类DefaultSqlSession的实例的创建，通过读取配置文件，获取对应的事务工厂，若没有设置，则默认采用容器的事务管理工厂。通过配置文件设置，创建对应的执行器Executor，最后创建sqlSession实例

##### 2.1 事务工厂的创建

Mybatis事务工厂接口TransactionFactory有两个实现类

- ManagedTransactionFactory 对应的事务为 ManagedTransaction 其事务交由容器进行管理，连接connection从datasource中获取，commit等方法没有操作，为空
- JdbcTransactionFactory 对应的事务为 JdbcTransaction 其事务交由数据库进行管理，连接connection从datasource中获取，commit等方法本质是由connection进行代理执行。
- SpringManagedTransactionFactory 对应事务为 SpringManagedTransaction，为Spring-mybatis。jar包中，非mybatis原生事务。

```java
public class JdbcTransactionFactory implements TransactionFactory {

  @Override
  public void setProperties(Properties props) {
  }

  @Override
  public Transaction newTransaction(Connection conn) {
    return new JdbcTransaction(conn);
  }

  @Override
  public Transaction newTransaction(DataSource ds, TransactionIsolationLevel level, boolean autoCommit) {
    return new JdbcTransaction(ds, level, autoCommit);
  }
}
```
##### 2.2 Executor的创建

Executor是mybatis运行的核心，sqlSession内部的执行基本通过调度器Executor进行执行，通过Executor来调度StatementHandler、ParameterHandler和ResultSetHandler，四者合称mybatis四大组件。
```java
public class Configuration{
     public Executor newExecutor(Transaction transaction, ExecutorType executorType) {
        executorType = executorType == null ? defaultExecutorType : executorType;
        executorType = executorType == null ? ExecutorType.SIMPLE : executorType;
        Executor executor;
        if (ExecutorType.BATCH == executorType) {
          executor = new BatchExecutor(this, transaction);
        } else if (ExecutorType.REUSE == executorType) {
          executor = new ReuseExecutor(this, transaction);
        } else {
          //默认为SimpleExecutor
          executor = new SimpleExecutor(this, transaction);
        }
        //如果开启了缓存，则为Executor添加缓存
        if (cacheEnabled) {
          executor = new CachingExecutor(executor);
        }
        //遍历所有插件，将executor进行赋予
        executor = (Executor) interceptorChain.pluginAll(executor);
        return executor;
      }    
}
```
Executor继承图如下：

#####  贴图


##### 2.3 SqlSession实例的创建

SqlSession本身的 select update等方法的实现，内部是Executor作为代理执行
```java
public class DefaultSqlSession{
    
     @Override
      public void select(String statement, Object parameter, RowBounds rowBounds, ResultHandler handler) {
        try {
          MappedStatement ms = configuration.getMappedStatement(statement);
          executor.query(ms, wrapCollection(parameter), rowBounds, handler);
        } catch (Exception e) {
          throw ExceptionFactory.wrapException("Error querying database.  Cause: " + e, e);
        } finally {
          ErrorContext.instance().reset();
        }
      }
    
     @Override
      public <E> List<E> selectList(String statement, Object parameter, RowBounds rowBounds) {
        try {
          MappedStatement ms = configuration.getMappedStatement(statement);
          return executor.query(ms, wrapCollection(parameter), rowBounds, Executor.NO_RESULT_HANDLER);
        } catch (Exception e) {
          throw ExceptionFactory.wrapException("Error querying database.  Cause: " + e, e);
        } finally {
          ErrorContext.instance().reset();
        }
      }
      // {.................................................}  
}
```


#### 总结

####### 贴图

#### 3.SqlSession的执行

##### 3.1 mybatis基于select update insert delete的方法查询

###### 3.1.1 查询流程

sqlSession的执行方法，本质是executor进行代理执行
```java
public class DefaultSqlSession{
      @Override
        //parameter 为参数， RowBounds为逻辑分页
      public <E> List<E> selectList(String statement, Object parameter, RowBounds rowBounds) {
        try {
           //根据mapperStatementId获取对应的ms
          MappedStatement ms = configuration.getMappedStatement(statement);
          //由Executor代理进行执行
          return executor.query(ms, wrapCollection(parameter), rowBounds, Executor.NO_RESULT_HANDLER);
        } catch (Exception e) {
          throw ExceptionFactory.wrapException("Error querying database.  Cause: " + e, e);
        } finally {
          ErrorContext.instance().reset();
        }
      }    
}
```
```java
public class BaseExecutor{
      @Override
      public <E> List<E> query(MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler resultHandler) throws SQLException {
        BoundSql boundSql = ms.getBoundSql(parameter);
        //创建cacheKey，用作缓存的Key
        CacheKey key = createCacheKey(ms, parameter, rowBounds, boundSql);
        return query(ms, parameter, rowBounds, resultHandler, key, boundSql);
     }    
    
      @Override
      public <E> List<E> query(MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler resultHandler, CacheKey key, BoundSql boundSql) throws SQLException {
        ErrorContext.instance().resource(ms.getResource()).activity("executing a query").object(ms.getId());
        if (closed) {
          throw new ExecutorException("Executor was closed.");
        }
        //当前查询请求为0时，清空缓存，直接从数据库查询，避免脏数据
        //localCache和localOutPutParameterCache为BaseExecutor的成员，构成了mybatis的一级缓存，即基于sqlSession的缓存，默认开启
        if (queryStack == 0 && ms.isFlushCacheRequired()) {
          clearLocalCache();
        }
        List<E> list;
        try {
          //记录当前查询的并发
          queryStack++;
          list = resultHandler == null ? (List<E>) localCache.getObject(key) : null;
          if (list != null) {
            //缓存命中，从缓存中获取
            handleLocallyCachedOutputParameters(ms, key, parameter, boundSql);
          } else {
            //否则从数据库中查询
            list = queryFromDatabase(ms, parameter, rowBounds, resultHandler, key, boundSql);
          }
        } finally {
          queryStack--;
        }
        //当所有查询语句结束时，，从缓存中取出执行结果
        if (queryStack == 0) {
          for (DeferredLoad deferredLoad : deferredLoads) {
            //延迟加载从缓存中获取结果
            deferredLoad.load();
          }
          // issue #601
          deferredLoads.clear();
          if (configuration.getLocalCacheScope() == LocalCacheScope.STATEMENT) {
            // issue #482
            //statement级别的缓存，只缓存相同id的sql，当所有的查询和延迟加载结束后，清空缓存，节省内存空间
            clearLocalCache();
          }
        }
        return list;
      }
    
      private <E> List<E> queryFromDatabase(MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler resultHandler, CacheKey key, BoundSql boundSql) throws SQLException {
        List<E> list;
        //利用占位符，先进行缓存占位
        localCache.putObject(key, EXECUTION_PLACEHOLDER);
        try {
        //开始查询数据库，该方法由子类实现，不同子类的实现方式不同
          list = doQuery(ms, parameter, rowBounds, resultHandler, boundSql);
        } finally {
          localCache.removeObject(key);
        }
        //查询结果放入缓存
        localCache.putObject(key, list);
        if (ms.getStatementType() == StatementType.CALLABLE) {
          localOutputParameterCache.putObject(key, parameter);
        }
        return list;
      }
}
```
```java
public class SimpleExecutor extends BaseExecutor {
      @Override
      public <E> List<E> doQuery(MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) throws SQLException {
        Statement stmt = null;
        try {
          Configuration configuration = ms.getConfiguration();
          //构建实例，表面为代理类RoutingStatementHandler的构建，但其只是门面类。
          StatementHandler handler = configuration.newStatementHandler(wrapper, ms, parameter, rowBounds, resultHandler, boundSql);
        //对StatementHandler进行初始化  
        stmt = prepareStatement(handler, ms.getStatementLog());
          return handler.<E>query(stmt, resultHandler);
        } finally {
          closeStatement(stmt);
        }
      }    
    
    //对StatementHandler进行初始化
     private Statement prepareStatement(StatementHandler handler, Log statementLog) throws SQLException {
        Statement stmt;
        Connection connection = getConnection(statementLog);
        //这里本质是调用的是StatementHandler的父类BaseStatementHandler的方法
        stmt = handler.prepare(connection);
        //SimpleParameter不做处理，PreparedStatementHandler会进行预处理，并且涉及到ParameterHandler的使用
        handler.parameterize(stmt);
        return stmt;
      }
}

public class Configuration{
      public StatementHandler newStatementHandler(Executor executor, MappedStatement mappedStatement, Object parameterObject, RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) {
        StatementHandler statementHandler = new RoutingStatementHandler(executor, mappedStatement, parameterObject, rowBounds, resultHandler, boundSql);
        statementHandler = (StatementHandler) interceptorChain.pluginAll(statementHandler);
        return statementHandler;
      }
}

public class RoutingStatementHandler implements StatementHandler {
     private final StatementHandler delegate;
     //构建实际具体的StatementHandler
      public RoutingStatementHandler(Executor executor, MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) {
    
        switch (ms.getStatementType()) {
          case STATEMENT:
            delegate = new SimpleStatementHandler(executor, ms, parameter, rowBounds, resultHandler, boundSql);
            break;
          case PREPARED:
            delegate = new PreparedStatementHandler(executor, ms, parameter, rowBounds, resultHandler, boundSql);
            break;
          case CALLABLE:
            delegate = new CallableStatementHandler(executor, ms, parameter, rowBounds, resultHandler, boundSql);
            break;
          default:
            throw new ExecutorException("Unknown statement type: " + ms.getStatementType());
        }
    
      }
    
        // 代理类方法的调用，本质是具体实现类的方法的调用
      @Override
      public int update(Statement statement) throws SQLException {
        return delegate.update(statement);
      }   
}

public abstract class BaseStatementHandler implements StatementHandler {
      @Override
      public Statement prepare(Connection connection) throws SQLException {
        ErrorContext.instance().sql(boundSql.getSql());
        Statement statement = null;
        try {
          //创建并初始化产生Statement，其由子类进行方法的实现
          statement = instantiateStatement(connection);
          //设置超时时间和fetchSize获取数据库的行数
          setStatementTimeout(statement);
          setFetchSize(statement);
          return statement;
        } catch (SQLException e) {
          closeStatement(statement);
          throw e;
        } catch (Exception e) {
          closeStatement(statement);
          throw new ExecutorException("Error preparing statement.  Cause: " + e, e);
        }
      }    
}

```
```java
public class SimpleStatementHandler extends BaseStatementHandler {
      @Override
      protected Statement instantiateStatement(Connection connection) throws SQLException {
        if (mappedStatement.getResultSetType() != null) {
        //通过connection创建Statement
          return connection.createStatement(mappedStatement.getResultSetType().getValue(), ResultSet.CONCUR_READ_ONLY);
        } else {
          return connection.createStatement();
        }
      }   
    
        @Override
        public <E> List<E> query(Statement statement, ResultHandler resultHandler) throws SQLException {
          String sql = boundSql.getSql();
         //最后本质的执行是通过JDBC的statement进行执行
          statement.execute(sql);
          //处理查询结果，并返回
          return resultSetHandler.<E>handleResultSets(statement);
        }
}
```
#### 插图

configuration实例化StatementHandler,创建的是RoutingStatementHandler，但其本质只是门面类，其构造方法内部通过参数判断，
创建了具体的StatementHandler子类实例，RoutingStatementHandler的方法的调用，本质上是具体实例化的StatementHandler的子类的调用。 

总的来说 SqlSession查询的流程就是executor代理进行执行，通过mapperStatement的id拿到对应mapperStatement，然后再拿到内部已经解析好的BoundSql，获取到最终的查询sql，
另外configuration通过mapperStatement创建对应的statementHandler实例，通过statementHandler再拿到对应的Statement，底层最终通过JDBC的statement执行sql完成数据查询。
当然期间还涉及缓存的流程，以及针对不同的StatemntHandler涉及不同的组件，比如ParameterHandler等。

###### 3.1.2 数据转换流程（ResultSetHandler）

数据库的查询结果已经通过JDBC的查询拿到为ResultSet，ResultSetHandler组件的作用就是将其转换为Java的POJO,其默认实现为DefaultResultSetHandler，允许用户通过配置进行插件覆盖。

```java
public class DefaultResultSetHandler{
      @Override
      public List<Object> handleResultSets(Statement stmt) throws SQLException {
        ErrorContext.instance().activity("handling results").object(mappedStatement.getId());
    
        final List<Object> multipleResults = new ArrayList<Object>();
    
        int resultSetCount = 0;
        //从JDBC数据库的Statement中取出结果ResultSet
        ResultSetWrapper rsw = getFirstResultSet(stmt);
        //resultMap定义了数据库列和java属性之间的映射关系，初始化mapperStatement时存储到其中
        List<ResultMap> resultMaps = mappedStatement.getResultMaps();
        int resultMapCount = resultMaps.size();
        validateResultMapsCount(rsw, resultMapCount);
        //遍历处理resultSet
        while (rsw != null && resultMapCount > resultSetCount) {
          //拿到resultMap
          ResultMap resultMap = resultMaps.get(resultSetCount);
          //将resultMap和resultSet结合，进行数据库到JAVA之间的映射
          handleResultSet(rsw, resultMap, multipleResults, null);
          //获取下一条resultSet
          rsw = getNextResultSet(stmt);
         //清空嵌套的Result结果集
          cleanUpAfterHandlingResultSet();
          resultSetCount++;
        }
    
        // 4 处理嵌套的resultMap，即映射结果中的某些子属性也需要resultMap映射时
        String[] resultSets = mappedStatement.getResulSets();
        if (resultSets != null) {
          while (rsw != null && resultSetCount < resultSets.length) {
            ResultMapping parentMapping = nextResultMaps.get(resultSets[resultSetCount]);
            if (parentMapping != null) {
              String nestedResultMapId = parentMapping.getNestedResultMapId();
              ResultMap resultMap = configuration.getResultMap(nestedResultMapId);
              handleResultSet(rsw, resultMap, null, parentMapping);
            }
            rsw = getNextResultSet(stmt);
            cleanUpAfterHandlingResultSet();
            resultSetCount++;
          }
        }
        //构造成list，将处理后的结果返回
        return collapseSingleResultList(multipleResults);
      }    

      private void handleResultSet(ResultSetWrapper rsw, ResultMap resultMap, List<Object> multipleResults, ResultMapping parentMapping) throws SQLException {
        try {
          if (parentMapping != null) {
            handleRowValues(rsw, resultMap, null, RowBounds.DEFAULT, parentMapping);
          } else {
            if (resultHandler == null) {
              DefaultResultHandler defaultResultHandler = new DefaultResultHandler(objectFactory);
              handleRowValues(rsw, resultMap, defaultResultHandler, rowBounds, null);
              multipleResults.add(defaultResultHandler.getResultList());
            } else {
              handleRowValues(rsw, resultMap, resultHandler, rowBounds, null);
            }
          }
        } finally {
          // issue #228 (close resultsets)
          closeResultSet(rsw.getResultSet());
        }
      }
}
```

关于结果映射处理这一块，在configuration构建StatementHandler时，通过StatementHandler的基类BaseStatementHandler的构造方法，完成了ResultSetHandler的初始化
```java
public abstract class BaseStatementHandler implements StatementHandler {
    protected BaseStatementHandler(Executor executor, MappedStatement mappedStatement, Object parameterObject, RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) {
        this.configuration = mappedStatement.getConfiguration();
        this.executor = executor;
        this.mappedStatement = mappedStatement;
        this.rowBounds = rowBounds;
    
        this.typeHandlerRegistry = configuration.getTypeHandlerRegistry();
        this.objectFactory = configuration.getObjectFactory();
    
        if (boundSql == null) { // issue #435, get the key before calculating the statement
          generateKeys(parameterObject);
          boundSql = mappedStatement.getBoundSql(parameterObject);
        }
    
        this.boundSql = boundSql;
    
        this.parameterHandler = configuration.newParameterHandler(mappedStatement, parameterObject, boundSql);
        this.resultSetHandler = configuration.newResultSetHandler(executor, mappedStatement, rowBounds, parameterHandler, resultHandler, boundSql);
      }    
}
```
在StatementHandler的实现类完成数据库查询后，拿到了ResultSet，这时需要通过ResultSetHandler进行数据映射处理，通过mapperStatement获取到配置的ResultMap，即数据库列与java pojo之间的映射关系，再通过statement获取到sql执行结果集
ResultSet，然后遍历结果集进行转化组装。包括处理嵌套映射等等，最终构造成List，将处理后的结果集返回。

#####总结 

mybatis的执行过程，本质就是其四大组件的配置使用，Executor负责调度 statementHandler负责statement的产生和执行，ParameterHandler负责statement在过程中的转化，
最终由ResultSetHandler完成数据库数据到Java POJO的映射和转化。

