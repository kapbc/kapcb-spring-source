
## # 基于`Gradle`搭建`Spring 5.5.13-release`源码阅读环境

> Spring版本：5.3.13-release



#### # 1、安装`JDK`

首先需要保证本地已经安装`JDK1.8`及以上版本。这里不做过多赘述，自行安装即可。

---

#### # 2、安装`Gradle`

- `Spring 5.x`开始全部都采用`Gradle`进行编译，构建源码前需要提前安装`Gradle`。`Gradle`官网下载地址为：[Gradle官网地址](https://gradle.org/releases)。

- 我这里使用的版本是最新的`gradle-7.3.1`。下载链接为：[Gradle-7.3.1下载地址](https://gradle.org/next-steps/?version=7.3.1&amp;amp;format=all)。

- 下载完成之后解压到文件夹。

- 配置`Gradle`环境变量，在环境变量的**系统变量**中添加如下：

  ```shell
  $	GRADLE_HOME
  
  $	D:\develop\IDE-Gradle\gradle-7.3.1
  ```

![gradle配置环境变量](https://img-blog.csdnimg.cn/b100b262a0de4936904505f2a226b1e8.png#pic_center)


- 在系统环境变量的`path`中添加环境变量：

  ```shell
  $	%GRADLE_HOME%\bin
  ```
![path系统环境变量添加gradle环境变量](https://img-blog.csdnimg.cn/dc535865f13148cfa96e70990eac0325.png?x-oss-process=image/watermark,type_d3F5LXplbmhlaQ,shadow_50,text_Q1NETiBAa2FwY2I=,size_20,color_FFFFFF,t_70,g_se,x_16#pic_center)


- 检测环境，使用`gradle -v`命令在`Terminal`中查看：

  ```shell
  $	gradle -v
  ```

  显示如下图则代表`gradle`安装成功。

![检测gradle是否安装成功](https://img-blog.csdnimg.cn/445747ba2c3f426682e2d6786c61a463.png?x-oss-process=image/watermark,type_d3F5LXplbmhlaQ,shadow_50,text_Q1NETiBAa2FwY2I=,size_20,color_FFFFFF,t_70,g_se,x_16#pic_center)


- 在`gradle`安装目录下的`init.d`文件夹下新建一个`init.gradle`文件，加入如下配置：
    - `repositories`中配置获取依赖`jar`包的顺序：
    - 先是从本地`maven`仓库中获取
    - 然后`mavenLocal()`是获取maven本地仓库的路径，和第一条一样，但是不冲突
    - 第三条第四条分别为国内`alibaba`镜像仓库和国外`bstek`镜像仓库
    - 最后`mavenCentral()`是从apache提供的中央仓库中获取依赖`jar`包

```properties
allprojects {
    repositories {
        maven { url 'file:///D:/develop/IDE-Repository'}
        mavenLocal()
        maven { name "Alibaba" ; url "https://maven.aliyun.com/repository/public" }
        maven { name "Bstek" ; url "https://nexus.bsdn.org/content/groups/public/" }
        mavenCentral()
    }

    buildscript { 
        repositories { 
		maven { url 'https://maven.aliyun.com/repository/google'}
        maven { url 'https://maven.aliyun.com/repository/jcenter'}
        maven { url 'https://maven.aliyun.com/nexus/content/groups/public'}
        }
    }
}
```

---

#### # 3、`Spring`版本命名规则

|          版本名称          |   版本   |                     版本意思                     |
| :------------------------: | :------: | :----------------------------------------------: |
|          snapshot          |  快照版  |            尚不稳定，处于开发中的版本            |
|          release           |  稳定版  | 功能相对稳定的版本，可以对外发行，但是有时间限制 |
| GA（General Availability） |  正式版  |               可广泛使用的稳定版本               |
|       M（Milestone）       | 里程碑版 |    具有一些全新的功能货时具有里程碑意义的版本    |
|  RC（Release Candidate）   | 最终测试 |            即将作为正式版本发布的版本            |

---

#### # 4、下载`Spring 5.3.13-release`源码

自`Spring 3.x`开始，`Spring`官方不在提供源码下载，所有源代码全部托管在`github`。[Spring Github托管代码首页地址](https://github.com/orgs/spring-projects/repositories?type=all)。

`spring framework 5.3.13-release`：

- 源码访问地址：`https://github.com/spring-projects/spring-framework/releases/tag/v5.3.13`

- 源码`zip`压缩包下载地址：`https://github.com/spring-projects/spring-framework/archive/refs/tags/v5.3.13.zip`

- 源码`tar.gz`压缩包下载地址：`https://github.com/spring-projects/spring-framework/archive/refs/tags/v5.3.13.tar.gz`
- 国内`gitee`托管源码访问地址：`https://gitee.com/mirrors/Spring-Framework?_from=gitee_search`
- 国内`getee`源码`zip`压缩包下载地址：`https://gitee.com/mirrors/Spring-Framework/repository/archive/v5.3.13`

下载完成之后解压即可。

---

#### # 5、修改`Spring`源码中`Gradle`配置

- 修改`Spring`源码的`Gradle`构建配置，在`Spring`源码下修改`gradle/wrapper/gradle-wrapper.properties`文件如下：

    - 修改`distributionUrl`为本地的`gradle`安装包路径，从本地拉去，加快源码构建编译的速度。

  ```properties
  distributionBase=GRADLE_USER_HOME
  distributionPath=wrapper/dists
  ## distributionUrl=https\://services.gradle.org/distributions/gradle-7.2-bin.zip
  ## 配置本地gradle, 配置之后需要配置gradle的中央仓库(阿里云maven中央仓库)
  distributionUrl=file:///D:/develop/IDE-Gradle/gradle-7.3.1-all.zip
  zipStoreBase=GRADLE_USER_HOME
  zipStorePath=wrapper/dists
  ```

- 设置阿里云镜像：

    - 修改根目录下`build.gradle`文件中的`repositories`：

  ```gradle
          repositories {
              // 配置本地maven仓库
              mavenLocal()
              // 配置阿里云maven仓库
              maven { url "https://maven.aliyun.com/nexus/content/groups/public/" }
              maven { url "https://maven.aliyun.com/nexus/content/repositories/jcenter/" }
              // maven中央仓库
              mavenCentral()
              maven { url "https://repo.spring.io/libs-spring-framework-build" }
          }
  ```

    - 修改根目录下`settings.gradle`文件中`pluginManagement`下的`repositories`：

  ```gradle
  pluginManagement {
  	repositories {
  		// 配置阿里云 maven 中央仓库
  		maven { url 'https://maven.aliyun.com/repository/public/' }
  		gradlePluginPortal()
  		maven { url 'https://repo.spring.io/plugins-release/' }
  	}
  }
  ```

    - 修改根目录下`gradle.properties`文件，将`jvmargs`根据自己本机内存重新分配内存大小，我这里分配了`10G`的内存：

  ```properties
  version=5.3.13
  org.gradle.jvmargs=-Xmx10240m
  org.gradle.caching=true
  org.gradle.parallel=true
  kotlin.stdlib.default.dependency=false
  ```

---

#### # 6、构建`Spring`源码

使用`Terminal`进入解压后`Spring`源码所在的文件目录下，预编译`spring-oxm`模块：

```shell
$	./gradlew :spring-oxm:compileTestJava
```

如某个`jar`包没下载成功等，只需要重新执行`./gradlew :spring-oxm:compileTestJava`再次进行预编译就行了。



构建完成之后修改根目录下`setting.gradle`文件，注释掉`spring-aspects`：

```properties
....
include "spring-aop"
// 移除aspects
// include "spring-aspects"
include "spring-beans"
include "spring-context"
....
```

---

#### # 7、导入IDEA

- 点击`File --> New --> Project from Existing Sources...`，然后选择`Spring`源码所在的目录

![导入Spring源码至IDEA](https://img-blog.csdnimg.cn/949d609085b847e3b494fbf470d66686.png?x-oss-process=image/watermark,type_d3F5LXplbmhlaQ,shadow_50,text_Q1NETiBAa2FwY2I=,size_18,color_FFFFFF,t_70,g_se,x_16#pic_center)


- 点击`ok`后选择使用`gradle`导入然后点击`Finish`

- 导入之后`IDEA`会自动进行构建，终止自动构建，此时还需要进行一些设置，`Ctrl+Alt+S`快捷键打开`IDEA`设置。

    - 点击`File | Settings | Build, Execution, Deployment | Compiler`，修改构建时堆内存大小（根据自己的电脑内存自行分配即可，我这里里是分配了`5G`）：
      ![修改IDEA编译堆内存大小](https://img-blog.csdnimg.cn/d79fabf7c3ac49b28b0a1bad9e32b27b.png?x-oss-process=image/watermark,type_d3F5LXplbmhlaQ,shadow_50,text_Q1NETiBAa2FwY2I=,size_20,color_FFFFFF,t_70,g_se,x_16#pic_center)

    - 点击`File | Settings | Build, Execution, Deployment | Build Tools | Gradle`，配置`IDEA`的`Gradle`配置：
      ![配置IDEA的Gradle](https://img-blog.csdnimg.cn/7fe87a289061483fb05cf67a7a6cb20c.png?x-oss-process=image/watermark,type_d3F5LXplbmhlaQ,shadow_50,text_Q1NETiBAa2FwY2I=,size_20,color_FFFFFF,t_70,g_se,x_16#pic_center)

    - `Ctrl+Alt+Shift+S`快捷键打开`Project Structure`。修改工程的`SDK`，分别将`Project`、`Modules`、`SDKs`中的`JDK`设置为本地安装的`JDK`（版本为1.8及以上）。
    - `Ctrl+Alt+Shift+S`快捷键打开`Project Structure`。在`Modules`中排除`spring-aspects`：
      ![排除spring-aspects](https://img-blog.csdnimg.cn/57f9382027f84c879d9e10c48b444e7a.png?x-oss-process=image/watermark,type_d3F5LXplbmhlaQ,shadow_50,text_Q1NETiBAa2FwY2I=,size_20,color_FFFFFF,t_70,g_se,x_16#pic_center)

    - `Alt+F12`快捷键打开`Terminal`，使用`gradlew.bat`命令进行编译

  ```shell
  $	gradlew.bat
  ```

  编译成功后会出现`BUILD SUCCESSFUL`的提示。如果有报错根据报错信息进行处理，多编译几次即可。



编译成功后整个工程如下图所示：
![spring构建成功](https://img-blog.csdnimg.cn/191b46f981584c1ea7149d76dbd5b4be.png?x-oss-process=image/watermark,type_d3F5LXplbmhlaQ,shadow_50,text_Q1NETiBAa2FwY2I=,size_20,color_FFFFFF,t_70,g_se,x_16#pic_center)


使用`shift+shift`快捷键输入`ApplicationContext`类，使用`Ctrl+Shift+Alt+U`如果能够成功打开类图，也证明`Spring`源码构建成功：

![构建成功类图](https://img-blog.csdnimg.cn/6058d23057b04cfa96366da5f6e320f4.png?x-oss-process=image/watermark,type_d3F5LXplbmhlaQ,shadow_50,text_Q1NETiBAa2FwY2I=,size_20,color_FFFFFF,t_70,g_se,x_16#pic_center)
---
#### # 8、创建Spring源码`debug`调试模块

- 使用`gradle`创建一新的`debug`模块。

- 创建完成之后将新建模块的`build.gradle`修改为：新建的模块名称。如我新建的模块叫`spring-context-debug`，则将其修改为：`spring-context-debug.gradle`并在其中配置：

```gradle
plugins {
    id 'java'
}

group 'org.springframework'
version '5.3.13'

repositories {
    // 配置本地 maven 仓库
    mavenLocal()
    // 配置阿里云 maven 仓库
    maven { url 'https://maven.aliyun.com/nexus/content/groups/public/' }
    maven { url 'https://maven.aliyun.com/nexus/content/repositories/jcenter' }
    // 配置 maven 中央仓库
    mavenCentral()
}

dependencies {
    // 测试需要依赖
    testImplementation 'org.junit.jupiter:junit-jupiter-api:5.7.0'
    testRuntimeOnly 'org.junit.jupiter:junit-jupiter-engine:5.7.0'
    // 导入 spring-context模块, 包含bean工厂
    implementation(project(':spring-context'))
    // 导入 spring-instrument 模块, 此模块为 spring-context 模块编译所必须的
    implementation(project('::spring-instrument'))
//    compile已经被gradle 7.x 弃用, 爷也是醉了
//    compile(project(":spring-context"))
}

test {
    useJUnitPlatform()
}
```

- 为新建模块添加`spring-context`和`spring-instrument`依赖。配置完成之后刷新`gradle`。如下图所示，`gradle`中新建模块的`runtimeClasspath`中已经新增`project spring-conetxt`和`project spring-instrument`代表以来成功。
  ![在这里插入图片描述](https://img-blog.csdnimg.cn/034daf97121f475597eb9982e8b7ae43.png?x-oss-process=image/watermark,type_d3F5LXplbmhlaQ,shadow_50,text_Q1NETiBAa2FwY2I=,size_20,color_FFFFFF,t_70,g_se,x_16#pic_center)

- 创建一个`TestBean`实现`DebugBean`

`DebugBean.java`：

```java
package com.kapcb.ccc.model;

/**
 * <a>Title: Bean </a>
 * <a>Author: Kapcb <a>
 * <a>Description: Bean <a>
 *
 * @author Kapcb
 * @version 1.0
 * @date 2021/12/11 23:21
 * @since 1.0
 */
public interface DebugBean {

	/**
	 * say method
	 */
	void say();

}

```



`TestBean.java`：

```java
package com.kapcb.ccc.model;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * <a>Title: TestBean </a>
 * <a>Author: Kapcb <a>
 * <a>Description: TestBean <a>
 *
 * @author Kapcb
 * @version 1.0
 * @date 2021/12/11 23:21
 * @since 1.0
 */
public class TestBean implements DebugBean {

	protected final Log log = LogFactory.getLog(getClass());

	private String username;

	public TestBean() {
		log.info("调用TestBean无参构造器");
	}

	@Override
	public void say() {
		log.info("Hi, I'm " + this.username);
	}

	public void setUsername(String username) {
		log.info("调用TestBean中的setUsername方法注入属性");
		this.username = username;
	}

}

```

- 添加测试代码，使用注解模式声明一个`Bean`：

```java
package com.kapcb.ccc.configuration;

import com.kapcb.ccc.model.DebugBean;
import com.kapcb.ccc.model.TestBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

/**
 * <a>Title: AnnotationTestConfiguration </a>
 * <a>Author: Kapcb <a>
 * <a>Description: AnnotationTestConfiguration <a>
 *
 * @author Kapcb
 * @date 2021/12/15 16:06
 */
@Configuration
public class AnnotationTestConfiguration {

	@Bean("testBean")
	@Scope("singleton")
	public DebugBean debugBean() {
		TestBean testBean = new TestBean();
		testBean.setUsername("Kapcb(Annotation)");
		return testBean;
	}

}
```

- 添加测试代码：

```java
package com.kapcb.ccc;

import com.kapcb.ccc.configuration.AnnotationTestConfiguration;
import com.kapcb.ccc.model.DebugBean;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * <a>Title: AnnotationApplication </a>
 * <a>Author: Kapcb <a>
 * <a>Description: AnnotationApplication <a>
 *
 * @author Kapcb
 * @date 2021/12/15 16:05
 */
public class AnnotationApplication {

	public static void main(String[] args) {
		BeanFactory beanFactory = new AnnotationConfigApplicationContext(AnnotationTestConfiguration.class);
		DebugBean testBean = beanFactory.getBean("testBean", DebugBean.class);
		testBean.say();
	}
}

```

使用`debug`模式运行，如果`debug`模式启动`main`方法报错，尝试修改：`File | Settings | Build, Execution, Deployment | Debugger | Data Views | Kotlin`在Disable coroutine agent前勾选后保存。

启动成功后控制台会输出如下日志：

```java
Connected to the target VM, address: '127.0.0.1:55594', transport: 'socket'
一月 06, 2022 2:56:42 下午 com.kapcb.ccc.model.TestBean <init>
信息: 调用TestBean无参构造器
一月 06, 2022 2:56:42 下午 com.kapcb.ccc.model.TestBean setUsername
信息: 调用TestBean中的setUsername方法注入属性
一月 06, 2022 2:56:42 下午 com.kapcb.ccc.model.TestBean say
信息: Hi, I'm Kapcb(Annotation)
Disconnected from the target VM, address: '127.0.0.1:55594', transport: 'socket'
```


附：

[github源码地址](https://github.com/kapbc/kapcb-spring-source/tree/master/Spring-Framework-v5.3.13)：`
https://github.com/kapbc/kapcb-spring-source/tree/master/Spring-Framework-v5.3.13`

