# android_integrate_smb
In android platform, integrate smb client and play media item in smb servers.

1. use jcifs open source, http://jcifs.samba.org/
2. the jcifs version is 1.2.25. In this repository, the jcifs-1.2.25.jar file is compiled by myself, which is patched by LargeReadWrite.patch
3. HttpServer.java is based on NanoHTTPD open source.
4. AsyncHttpService.java is based on AsyncHttp open source.
5. why use http server?
   Because MediaPlayer in android can't play smb protocal files directly, http server covert http to smb protocal.
6. How to build jcifs open source.
  (0) download jcifs source code. http://jcifs.samba.org/src/
  (1) download JavaEE sdk, add javax.servlet-api.jar in this sdk into class path of the operation system.
  (2) modify the javac tool path in build.xml according to your operation system.
  (3) use ant to compile.
