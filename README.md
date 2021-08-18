# 基于Spring的Controller的YAPI接口文档生成器

自动扫描Java上面的代码生成OpenAPI文档，无代码侵入。

对比与Swageer的代码侵入显得更加简洁。

## 诞生原因

在开发的过程中，尤其是联调的过程中，接口出入参的修改是很频繁的一件事。这就导致开发过程中，修改了接口参数缺忘记修改接口文档。
因此需要一个工具能够自动读取Java类中的注释来生成文档。

## 对比Swagger

Springfox的代码侵入性太强了，使得代码一点优雅性都没有，注解上写了说明那还要写注释么？
而且Springfox的功能太强大，几乎能够生成所有类型的文档，但实际使用中最多的还是使用JSON作为DSL来描述，所以这个项目目前只支持了使用JSON进行出入参描述。

## 基本特性

yapidoc最终会把扫描路径下的所有controller解析成OpenApi 3.0协议。

controller解析:会扫描带有@Controller和@RestController的类。

URL解析:SpringMvc中定义的基本Mapping

入参解析: 如果使用@RequestBody注解则解析成JSON格式、否则就解析成param格式。

出参解析:会把所有的类都解析成JSON格式。

## Maven插件使用方式

插件引入：

~~~xml
<plugin>
    <artifactId>yapidoc-maven-plugin</artifactId>
    <groupId>com.eeeffff.yapidoc</groupId>
    <version>1.0.0</version>
    <configuration>
        <!--Yapi远程服务器地址-->
        <yapiUrl>http://yapi.xxx.com/</yapiUrl>
        <!-- 修改成特定项目的Token -->
        <yapiProjectToken>2de6586ab2c8074ff5b3002b5c2132e3c7efef9422d324955eba0a2f6cfc0dd9</yapiProjectToken>
        <!-- 指定只需要解析的Controller的名称，不需要指定包名，多个以英文逗号分隔，可选参数 -->
        <controllers><controllers>
        <!-- 指定只需要解析Controller所在的包名，多个以英文逗号分隔，可选参数 -->
        <packages><packages>
    </configuration>
    <executions>
        <execution>
            <id>doc-dependencies</id>
            <phase>prepare-package</phase>
            <goals>
                <goal>yapidoc</goal>
            </goals>
        </execution>
    </executions>
</plugin>
~~~
插件默认为prepare-package生命周期，运行有两种方式：

```shell
mvn yapidoc:yapidoc
```

或

```shell
mvn prepare-package -Dmaven.test.skip
```

