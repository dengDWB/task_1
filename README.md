# 加载本地html功能点技术文档 #



## 项目目的 ##

project为了解决加载本地html，引用本地的img、css、javascript。并且网页更新则再次下载解析html，显示解析完的html，否则直接加载本地html.


## 项目文字描述的基本逻辑 ##

1.先判断文件下载到的文件夹是否存在，不存在则创建，

2.设置浏览器的相关配置

3.注册多文件断点下载，记得在onDestory()中销毁。实现多文件断点下载想使用的相关方法，我们使用了下载完成的方法，

4.写一个利用okhttp相关知识得到网页源代码和header信息的方法，记住在使用okhttp3的时候一定要设置缓存，在设备的缓存文件夹中有一个文件缓存header的相关信息，把得到的网页源代码传递到一个方法中

5.写一个解析html的方法，把上一步得到网页源代码传递到解析html方法中，解析html方法主要是解析img、css、js,把解析这个三个元素中的东西，写三个方法。

6.在三个方法中做了一个相同的事情，得到想知道元素的值（下载文件的链接），我们利用这个值，可以截取到我们想知道的文件名字和判断是相对路径还是完整的路径。我们把得到的值替换本地资源指引的路径，在替换之前我们要把一些值传入到下载方法中，

而下载方法中对下载文件分类，并对不同类别的文件下载到不同的文件夹中，所以传递的参数有下载链接、下载文件的类别、下载文件的名字.在这个方法启动以后，记得要保存文档到指定位置。在解析完后，再启动浏览器加载本地html。在这个点之前，我们的逻辑是只要启动，就是从okhttp3到保存文档这一块是下载网页源代码解析网页在显示网页，现在我们要设置更新则重复这个逻辑，不更新直接加载本地html。


7，我们在利用okhttp这个方法中，从header信息中可以得到ETag、Last_Modified,注意（有些网页（越大型的公司）的这两个值是不会暴露出来的，可能都没有这两个值，可能只有其中一个，如果得到其中有一个为null或者两个，把为null全部改成为""）。我们可以设置键值对存储得到两个这个值，从网上获得的两个值。

我们先得到本地的这两个参数，如果都是为null，直接再次解析网页，把本地的ETag、Last_Modified替换成网上的，

如果不是我们直接判断网上的这两个参数是否为空，如果都是为null也是直接再次解析网页，把本地的ETag、Last_Modified替换成网上的，

如果只是其中一个为null的话，就把不为null的那个参数和本地的比较，如果相等，网页没有更新，直接加载本地html，如果不想等的话，再次解析网页把本地的ETag、Last_Modified替换成网上的


## 逻辑描述代码方法参考 ##

方法简述（括号为传递的数据的类型）：

- ``isFileDir()``：判断文件夹和特定文件是否存在


- ``getHtmlInfo(String)``:网页源代码转成Document，并启动解析方法
- ``getImgLink(Document)``:提取Document中图片链接，把本地链接替换网上链接，并启动下载保存Document
- ``getStylesheetLink(Document)``:提取Document中样式链接，把本地链接替换网上链接，并启动下载保存Document
- ``getJavaScript(Document)``:提取Document中javascript脚本链接，把本地链接替换网上链接，并启动下载保存Document
- ``startDownload(String url, final String fileType, final String filesName)``：下载链接中的文件
- ``downloadHtml()``:得到网页body和header信息，并判断是否网页更新
- ``saveCache(String sTag, String lastModified)``存储键值对
- ``loadWebView()``：主线程中浏览器加载本地html
- ``saveIndexHtml()``:保存Document文档到指定文件夹中
- ``showDialog()``:显示对话框，提示是否网页更新

