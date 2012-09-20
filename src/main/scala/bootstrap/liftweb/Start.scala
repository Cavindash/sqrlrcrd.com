package bootstrap.liftweb

import java.io.File

import org.eclipse.jetty.server.Server
import org.eclipse.jetty.webapp.WebAppContext
import net.liftweb.util.Props

object Start {

  def main(args: Array[String]): Unit = {

    /* 
     * Use: java -Drun.mode=production -jar myjarname.jar
     * to change run.mode, which by default is development 
     */
    
    /* Calculate run.mode dependent path to logback configuration file.
     * Use same naming scheme as for props files.  */
    val logbackConfFile = {
      val propsDir = "props"
      val fileNameTail = "default.logback.xml"
      val mode = System.getProperty("run.mode")
      if (mode != null) propsDir + "/" + mode + "." + fileNameTail
      else propsDir + "/" + fileNameTail
    }
    /* set logback config file appropriately */
    System.setProperty("logback.configurationFile", logbackConfFile)

    /* choose different port for each of your webapps deployed on single server
     * you may use it in nginx proxy-pass directive, to target virtual hosts
     * line below will attempt to read jetty.emb.port property from
     * props file or use supplied default 9090 */
    val port = Props.getInt("jetty.emb.port", 9090)
    val server = new Server(port)
    val webctx = new WebAppContext
    /* use embedded webapp dir as source of the web content -> webapp
     * this is the dir within jar where we have put stuff with zip.
     * it was in a directory created by package-war, in target (also
     * named webapp), which was outside the jar. now, thanks to zip
     * it's inside so we need to use method bellow to get to it.
     * web.xml is in default location, of that embedded webapp dir,
     * so we don't have do webctx.setDescriptor */
    val webappDirInsideJar = webctx.getClass.getClassLoader.getResource("webapp").toExternalForm
    webctx.setWar(webappDirInsideJar)

    /* might use use external pre-existing webapp dir instead of referencing
     * the embedded webapp dir but it's not very useful. why would we put
     * webapp inside if we end up using some other external directory. I put it
     * for reference, may make sense under some circumstances.
     * webctx.setResourceBase("webapp") */

    webctx.setContextPath("/")
    /* optionally extract embedded webapp to specific temporary location and serve
     * from there. In fact /tmp is not a good place, because it gets cleaned up from
     * time to time so you need to specify some location such as /var/www/sqrlrcrd.com
     * for anything that should last */
    val shouldExtract = Props.getBool("jetty.emb.extract", false)
    if (shouldExtract) {
      val webtmpdir = Props.get("jetty.emb.tmpdir", "/tmp")
      webctx.setTempDirectory(new File(webtmpdir))
    }

    server.setHandler(webctx)
    server.start
    server.join

  }

}