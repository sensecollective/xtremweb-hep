/*
 * Copyrights     : CNRS
 * Author         : Oleg Lodygensky
 * Acknowledgment : XtremWeb-HEP is based on XtremWeb 1.8.0 by inria : http://www.xtremweb.net/
 * Web            : http://www.xtremweb-hep.org
 *
 *      This file is part of XtremWeb-HEP.
 *
 *    XtremWeb-HEP is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    XtremWeb-HEP is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with XtremWeb-HEP.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package xtremweb.dispatcher;

import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.URISyntaxException;
import java.nio.channels.DatagramChannel;
import java.rmi.RemoteException;
import java.security.AccessControlException;
import java.security.InvalidKeyException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.net.ssl.SSLSocket;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;

import xtremweb.common.BytePacket;
import xtremweb.common.DataInterface;
import xtremweb.common.DataTypeEnum;
import xtremweb.common.Logger;
import xtremweb.common.MD5;
import xtremweb.common.StatusEnum;
import xtremweb.common.StreamIO;
import xtremweb.common.Table;
import xtremweb.common.UID;
import xtremweb.common.UserInterface;
import xtremweb.common.UserRightEnum;
import xtremweb.common.XMLHashtable;
import xtremweb.common.XMLable;
import xtremweb.common.XWConfigurator;
import xtremweb.common.XWPropertyDefs;
import xtremweb.common.XWTools;
import xtremweb.communications.Connection;
import xtremweb.communications.IdRpc;
import xtremweb.communications.URI;
import xtremweb.communications.XMLRPCCommand;
import xtremweb.communications.XMLRPCCommandActivateHost;
import xtremweb.communications.XMLRPCCommandChmod;
import xtremweb.communications.XMLRPCCommandWorkAlive;
import xtremweb.communications.XWPostParams;
import xtremweb.database.SQLRequest;
import xtremweb.security.XWAccessRights;

/**
 * This handles incoming communications through TCP<br>
 * This answers request from TCPClient
 *
 * Created: August 2005
 *
 * @see xtremweb.communications.TCPClient
 * @author Oleg Lodygensky
 * @version RPCXW
 */

public class HTTPHandler extends xtremweb.dispatcher.CommHandler {

	public static final String PATH = "/";

	public static final String APIPATH = "/api";
	/**
	 * This is the name of the cookie containing the ser UID
	 */
	public static final String COOKIE_USERUID = "USERUID";

	/**
	 * This is the resource path in the xtremweb java archive (xtremweb.jar) :
	 * "data"
	 */
	public static final String RESOURCE_PATH = "data";
	/**
	 * This is the Bootstrap dashboard HTML file name : dashboard.html
	 *
	 * @since 10.2.0
	 */
	public static final String DASHBOARDFILENAME_HTML = "/dashboard.html";
	/**
	 * This is the Bootstrap dashboard CSS file name : dashboard.css
	 *
	 * @since 10.2.0
	 */
	public static final String DASHBOARDFILENAME_CSS = "/dashboard.css";
	/**
	 * This is the Bootstrap carousel HTML file name : carousel.html
	 *
	 * @since 10.2.0
	 */
	public static final String CAROUSELFILENAME_HTML = "/carousel.html";
	/**
	 * This is the Bootstrap carousel CSS file name : carousel.css
	 *
	 * @since 10.2.0
	 */
	public static final String CAROUSELFILENAME_CSS = "/carousel.css";
	/**
	 * This is the HTML file name : xwserver.html
	 */
	public static final String RESOURCEFILENAME_HTML = "/xwserver.html";
	/**
	 * This is the CSS file name : xwserver.css
	 */
	public static final String RESOURCEFILENAME_CSS = "/xwserver.css";
	/**
	 * This is a new HTML without JavaScript file name : xwserver2.html
	 *
	 * @since 10.2.0
	 */
	public static final String RESOURCEFILENAME2_HTML = "/xwserver2.html";
	/**
	 * This is the login page
	 *
	 * @since 10.2.0
	 */
	public static final String LOGIN_HTML = "/login.html";
	/**
	 * This is JavaScript -extracted from xwserver.html- file name :
	 * xwserver2.js
	 *
	 * @since 10.2.0
	 */
	public static final String SCRIPTFILENAME_JS = "/xwserver2.js";
	/**
	 * This is the favicon file name : favicon.ico
	 *
	 * @since 10.2.0
	 */
	public static final String FAVICOFILENAME_ICO = "/favicon.ico";
	/**
	 * This is the logo file name : logo.jpg
	 */
	public static final String RESOURCEFILENAME_LOGO = "/logo.jpg";

	private static final String[] names = {
			RESOURCEFILENAME_HTML,
			RESOURCEFILENAME_CSS,
			DASHBOARDFILENAME_HTML,
			DASHBOARDFILENAME_CSS,
			CAROUSELFILENAME_HTML,
			CAROUSELFILENAME_CSS,
			FAVICOFILENAME_ICO,
			RESOURCEFILENAME_LOGO,
			RESOURCEFILENAME2_HTML,
			SCRIPTFILENAME_JS,
			LOGIN_HTML
	};
	private static final String TEXTHTML = "text/html";
	private static final String TEXTCSS = "text/css";
	private static final String IMAGEXICON = "image/x-icon";
	private static final String IMAGEJPEG = "image/jpeg";
	private static final String APPJS = "application/javascript";
	private static final String[] mimeTypes = {
			TEXTHTML,
			TEXTCSS,
			TEXTHTML,
			TEXTCSS,
			TEXTHTML,
			TEXTCSS,
			IMAGEXICON,
			IMAGEJPEG,
			TEXTHTML,
			APPJS,
			TEXTHTML 
	};

	/**
	 * This enumerates resources needed by the dashboard html file (css, images,
	 * javascript)
	 *
	 * @since 8.3.0
	 */
	public enum Resources {
		XWHTML,
		XWCSS,
		/**
		 * This is the Bootstrap dashboard HTML
		 *
		 * @since 10.2.0
		 */
		DASHBOARDHTML,
		/**
		 * This is the Bootstrap carousel HTML
		 *
		 * @since 10.2.0
		 */
		DASHBOARDCSS,
		/**
		 * This is the Bootstrap dashboard CSS
		 *
		 * @since 10.2.0
		 */
		CAROUSELHTML,
		/**
		 * This is the Bootstrap dashboard CSS
		 *
		 * @since 10.2.0
		 */
		CAROUSELCSS,
		/**
		 * This is the favicon resource
		 *
		 * @since 10.2.0
		 */
		FAVICON,
		LOGO,
		/**
		 * This is a new HTML without JavaScript
		 *
		 * @since 10.2.0
		 */
		XWHTML2,
		/**
		 * This is JavaScript, extracted from xwserver.html
		 *
		 * @since 10.2.0
		 */
		XWJS,
		/**
		 * This is the login page
		 *
		 * @since 10.2.0
		 */
		LOGINPAGE;

		/**
		 * This retrieves the resource name started with a slash. This is both
		 * used to retrieve resource from the archive and as path for web access
		 *
		 * @return the resource name
		 */
		public String getName() {
			return names[this.ordinal()];
		}

		/**
		 * This retrieves the resource path in the xtremweb java archive
		 * (xtremweb.jar)
		 *
		 * @return the resource path in the archive
		 */
		public String getPath() {
			return RESOURCE_PATH + this.getName();
		}

		/**
		 * @return the resource path
		 */
		public String getMimeType() {
			return mimeTypes[this.ordinal()];
		}

		/**
		 * This writes resource content to the given response. This calls
		 * writeBinary() for non text resources
		 *
		 * @see #writeBinary(HttpServletResponse)
		 * @param response
		 *            is the output channel to write to
		 * @throws IOException
		 *             is thrown if resource is not found
		 */
		public void write(final HttpServletResponse response) throws IOException {
			final InputStream reader = getClass().getClassLoader().getResourceAsStream(getPath());

			if (reader == null) {
				throw new IOException(getPath() + " not found");
			}
			final OutputStream out = response.getOutputStream();
			final byte[] buf = new byte[10240];
			for (int n = reader.read(buf); n > 0; n = reader.read(buf)) {
				out.write(buf, 0, n);
			}
			out.flush();
		}

	}

	/** This is the content type label HTTP header */
	private static final String CONTENTTYPELABEL = "Content-Type";
	/** This is the content type default value : application/octet-stream */
	private static final String CONTENTTYPEVALUE = "application/octet-stream";
	/** This is the content length label HTTP header */
	private static final String CONTENTLENGTHLABEL = "Content-Length";
	/** This is the content md5sum label HTTP header */
	private static final String CONTENTMD5LABEL = "Content-MD5";
	/** This is the content disposition label HTTP header */
	private static final String CONTENTDISPOSITIONLABEL = "Content-Disposition";
	/** This is the last modified label HTTP header */
	private static final String LASTMODIFIEDLABEL = "Last-Modified";

	private final String DEFAULT_ANSWER_HEAD = "<html><head><title>XtremWeb-HEP API</title></head>"
			+ "<body><center><h1>XtremWeb-HEP API</h1><br /><h3>You are logged as \"%s\"</h3></center><br /><br /><a href=\"/\">Go to the client interface</a><br /><br />"
			+ "Available interface commands :<br /><ul>";
	private final String DEFAULT_UPLOAD_FORM = "<form action=\"%s/" + IdRpc.UPLOADDATA + "\""
			+ " enctype=\"multipart/form-data\" method=\"post\">"
			+ "<div style=\"margin:10px;padding:10px;background-color:lightgrey\"><p><h2>Upload form</h2></p>"
			+ "<p style=\"color:red\">You must have registered your data first</p>" + "<p>Registered Data UID:<br>"
			+ "<input type=\"text\" name=\"" + XWPostParams.DATAUID + "\" size=\"40\"></p><p>"
			+ "<p>Registered Data size:<br>" + "<input type=\"text\" name=\"" + XWPostParams.DATASIZE
			+ "\" size=\"40\"></p><p>" + "<p>Registered Data md5sum:<br>" + "<input type=\"text\" name=\""
			+ XWPostParams.DATAMD5SUM + "\" size=\"40\"></p><p>" + "Please specify a file:<br>"
			+ "<input type=\"file\" name=\"" + XWPostParams.DATAFILE + "\" size=\"45\"></p><div>"
			+ "<input type=\"submit\" value=\"Send\"></div></form>";

	private final String DEFAULT_ANSWER_TAIL = "</ul><br /><ul><li> to retrieve a specific objet : %s/get/a_specific_uid</ul></body></html>";

	private IdRpc idRpc;

	private synchronized void resetIdRpc() {
		getLogger().debug(Thread.currentThread().getId() + " : resetIdRpc");
		idRpc = null;
		notifyAll();
	}

	private synchronized void setIdRpc(final IdRpc i) {
		getLogger().debug(Thread.currentThread().getId() + " : setIdRpc " + i);
		idRpc = i;
		notifyAll();
	}

	private synchronized void waitIdRpc(final IdRpc i) {
		while (getIdRpc() != null) {
			try {
				getLogger().debug(Thread.currentThread().getId() + " : waitIdRpc " + i);
				wait();
				getLogger().debug(Thread.currentThread().getId() + " : waitIdRpc woken up");
			} catch (final InterruptedException e) {
			}
		}
		setIdRpc(i);
		notifyAll();
	}

	private synchronized IdRpc getIdRpc() {
		getLogger().warn(Thread.currentThread().getId() + " : getIdRpc a ete synchronise ! " + idRpc);
		getLogger().debug(Thread.currentThread().getId() + " : getIdRpc " + idRpc);
		return idRpc;
	}

	private HttpServletRequest request;
	private HttpServletResponse response;
	private FileItem dataUpload;
	private long dataUploadSize;
	private String dataUploadmd5sum;
	private final FileItemFactory diskFactory;
	private final ServletFileUpload servletUpload;

	public static final String NAME = "HTTPHandler";
	/**
	 * This is the email address header in the certificate distinguish name.
	 * <br />
	 * The certificate distinguish name looks like
	 * <code>EMAILADDRESS=lodygens@lal.in2p3.fr, CN=Oleg Lodygensky, OU=UMR8607, O=CNRS, C=FR</code>
	 *
	 * @since 8.3.0
	 */
	private static final String DNHEADER_EMAIL = "EMAILADDRESS=";
	private static final int DNHEADERLENGTH_EMAIL = DNHEADER_EMAIL.length();
	/**
	 * This is the common name header in the certificate distinguish name.<br />
	 *
	 * @see #DNHEADER_EMAIL
	 * @since 8.3.0
	 */
	private static final String DNHEADER_CN = "CN=";
	private static final int DNHEADERLENGTH_CN = DNHEADER_CN.length();
	/**
	 * This is the organization unit header in the certificate distinguish name.
	 * <br />
	 *
	 * @see #DNHEADER_EMAIL
	 * @since 8.3.0
	 */
	private static final String DNHEADER_OU = "OU=";
	private static final int DNHEADERLENGTH_OU = DNHEADER_OU.length();
	/**
	 * This is the organization header in the certificate distinguish name.
	 * <br />
	 *
	 * @see #DNHEADER_EMAIL
	 * @since 8.3.0
	 */
	private static final String DNHEADER_O = "O=";
	private static final int DNHEADERLENGTH_O = DNHEADER_O.length();
	/**
	 * This is the country header in the certificate distinguish name.<br />
	 *
	 * @see #DNHEADER_EMAIL
	 * @since 8.3.0
	 */
	private static final String DNHEADER_C = "C=";
	private static final int DNHEADERLENGTH_C = DNHEADER_C.length();

	/**
	 * This is the default constructor which only calls super(NAME)
	 */
	public HTTPHandler() {
		super(NAME);
		dataUpload = null;
		dataUploadSize = -1;
		diskFactory = new DiskFileItemFactory();
		servletUpload = new ServletFileUpload(diskFactory);
		servletUpload.setSizeMax(XWPostParams.MAXUPLOADSIZE);
		getLogger().debug("new Thread " + Thread.currentThread().getId());
	}

	/**
	 * This is the default constructor which only calls super(NAME)
	 */
	public HTTPHandler(final XWConfigurator c) {
		this(NAME, c);
	}

	/**
	 *
	 */
	public HTTPHandler(final String n, final XWConfigurator c) {
		super(n, c);
		dataUpload = null;
		dataUploadSize = -1;
		diskFactory = new DiskFileItemFactory();
		servletUpload = new ServletFileUpload(diskFactory);
		servletUpload.setSizeMax(XWPostParams.MAXUPLOADSIZE);
		getLogger().debug("new Thread " + Thread.currentThread().getId());
	}

	/**
	 * This constructor call the previous constructor
	 *
	 * @param socket
	 *            is not used
	 * @see #HTTPHandler(XWConfigurator)
	 */
	public HTTPHandler(final SSLSocket socket, final XWConfigurator c) throws RemoteException {
		this(c);
	}

	/**
	 * This does nothing
	 */
	@Override
	public void setSocket(final Socket s) throws RemoteException {
	}

	/**
	 * This does nothing
	 */
	@Override
	public void setSocket(final SSLSocket s) throws RemoteException {
	}

	/**
	 * This throws an exception since setPacket() is dedicated to UDP comms
	 *
	 * @exception RemoteException
	 *                is always thrown since this method is dedicated to UDP
	 *                comms
	 */
	@Override
	public void setPacket(final DatagramSocket s, final DatagramPacket p) throws RemoteException {
		throw new RemoteException("HTTPHandler#setPacket() TCP can't set packet");
	}

	/**
	 * @see xtremweb.communications.CommHandler#setSocket(Socket)
	 * @exception RemoteException
	 *                is always thrown since this method is dedicated to UDP
	 *                comms
	 */
	@Override
	public void setPacket(final DatagramChannel c, final SocketAddress r, final BytePacket p) throws RemoteException {
		throw new RemoteException("HTTPHandler#setPacket() TCP can't set packet");
	}

	/**
	 * This does nothing and must be overidden by any HTTP handler This is
	 * inherited from org.mortbay.jetty.Handler
	 */
	@Override
	public void setServer(final Server server) {
	}

	/**
	 * This does nothing and must be overidden by any HTTP handler This is
	 * inherited from org.mortbay.jetty.Handler
	 */
	@Override
	public Server getServer() {
		return null;
	}

	/**
	 * This does nothing and must be overidden by any HTTP handler This is
	 * inherited from org.mortbay.component.LifeCycle
	 *
	 * @return true
	 */
	@Override
	public boolean isFailed() {
		return true;
	}

	/**
	 * This does nothing and must be overidden by any HTTP handler This is
	 * inherited from org.mortbay.component.LifeCycle
	 *
	 * @return false
	 */
	@Override
	public boolean isRunning() {
		return false;
	}

	/**
	 * This does nothing and must be overidden by any HTTP handler This is
	 * inherited from org.mortbay.component.LifeCycle
	 *
	 * @return false
	 */
	@Override
	public boolean isStarted() {
		return false;
	}

	/**
	 * This does nothing and must be overidden by any HTTP handler This is
	 * inherited from org.mortbay.component.LifeCycle
	 *
	 * @return false
	 */
	@Override
	public boolean isStarting() {
		return false;
	}

	/**
	 * This does nothing and must be overidden by any HTTP handler This is
	 * inherited from org.mortbay.component.LifeCycle
	 *
	 * @return true
	 */
	@Override
	public boolean isStopped() {
		return true;
	}

	/**
	 * This does nothing and must be overidden by any HTTP handler This is
	 * inherited from org.mortbay.component.LifeCycle
	 *
	 * @return false
	 */
	@Override
	public boolean isStopping() {
		return false;
	}

	/**
	 * This does nothing and must be overidden by any HTTP handler This is
	 * inherited from org.mortbay.component.LifeCycle
	 */
	@Override
	public void start() {
	}

	/**
	 * This writes parameter to output channel
	 */
	@Override
	protected synchronized void write(final XMLable answer) throws IOException {

		final Logger logger = getLogger();
		mileStone("<write>");

		if (getIdRpc() == IdRpc.DOWNLOADDATA) {
			logger.debug("DOWNLOADDATA : don't write out");
			response.setStatus(HttpServletResponse.SC_OK);
			resetIdRpc();
			notifyAll();
			return;
		}

		if (response == null) {
			mileStone("<error method='write' msg='no response' />");
			mileStone("</write>");
			error("Can't write : this.response is not set");
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			resetIdRpc();
			notifyAll();
			return;
		}

		try {
			response.setDateHeader("Date", new Date().getTime());
			response.setContentType("text/xml;charset=UTF-8");
			response.getWriter().println(XMLable.XMLHEADER + "<" + XMLable.ROOTTAG + ">");
			if (answer != null) {
				debug("HTTPHandler#write(" + answer.toXml() + ")");
				response.getWriter().println(answer.toXml());
			}
		} catch (final Exception e) {
			logger.exception(e);
			mileStone("<error method='write' msg='" + e.getMessage() + "' />");
			throw new IOException(e.toString());
		} finally {
			response.setStatus(HttpServletResponse.SC_OK);
			response.getWriter().println("</" + XMLable.ROOTTAG + ">");
			resetIdRpc();
			mileStone("</write>");
			notifyAll();
		}
	}

	/**
	 * This sends the HTML page to the client
	 *
	 * @since 8.1.0
	 */
	private void usagePage(final UserInterface client) throws IOException {

		response.setContentType(TEXTHTML);
		final Cookie cookie = new Cookie(COOKIE_USERUID, client.getUID().toString());
		getLogger().debug("setCookie " + COOKIE_USERUID + " = " + client.getUID().toString());
		response.addCookie(cookie);
		Resources.XWHTML.write(response);
		response.setStatus(HttpServletResponse.SC_OK);
	}

	/**
	 * This sends the resource to the client
	 *
	 * @since 8.3.0
	 */
	private void sendResource(final Resources r) throws IOException {

		if (r.getMimeType() != null) {
			response.setContentType(r.getMimeType());
		}
		r.write(response);
		response.getWriter().flush();
		response.setStatus(HttpServletResponse.SC_OK);
	}

	/**
	 * This displays the page proposing to log using openid or to go to the
	 * statistics page This sends back the HTML status
	 * HttpServletResponse.SC_UNAUTHORIZED
	 *
	 * @since 8.2.0
	 */
	private void redirectPage(final Request baseRequest, final Resources res) throws IOException {
		getLogger().debug("redirectPage " + res);
		response.setContentType(TEXTHTML);
		response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		response.sendRedirect(res.getName());
		baseRequest.setHandled(true);
	}

	/**
	 * This writes the API to output stream
	 *
	 * @since 8.0.2
	 */
	protected synchronized void writeApi(final UserInterface client, final URI baseUri) throws IOException {

		if (response == null) {
			throw new IOException("Can't write : this.response is not set");
		}

		mileStone("<writeApi str='" + baseUri + "'>");

		try {
			final PrintWriter writer = response.getWriter();
			String msg = String.format(DEFAULT_ANSWER_HEAD,
					client.getEMail() != null ? client.getEMail() : client.getLogin());
			response.setContentType(TEXTHTML + ";charset=UTF-8");
			writer.println(msg);

			for (final IdRpc i : IdRpc.values()) {
				writer.println("<li> <a href=\"" + baseUri + "/" + i + "\">" + baseUri + "/" + i + "</a> : "
						+ i.helpRestApi());
			}

			msg = String.format(DEFAULT_UPLOAD_FORM, baseUri);
			writer.println(msg);
			msg = String.format(DEFAULT_ANSWER_TAIL, baseUri);
			writer.println(msg);
		} catch (final Exception e) {
			getLogger().exception(e);
			mileStone("<error method='writeapi' msg='" + e.getMessage() + "' />");
			throw new IOException(e.toString());
		} finally {
			response.setStatus(HttpServletResponse.SC_OK);
			resetIdRpc();
			mileStone("</writeApi>");
			notifyAll();
		}
	}

	/**
	 * This writes parameter to output channel
	 */
	protected synchronized <T extends Table> void writeRows(final T row) throws IOException {

		if (response == null) {
			throw new IOException("Can't write : this.response is not set");
		}
		mileStone("<writeRows>");

		Collection<T> rows = null;
		Iterator<T> rowsEnum = null;

		try {
			rows = DBInterface.getInstance().selectAllPublic(row);

			if (rows != null) {
				rowsEnum = rows.iterator();

				while (rowsEnum.hasNext()) {
					T r = rowsEnum.next();
					if (r instanceof UserInterface) {
						((UserInterface) r).setCertificate("****");
						((UserInterface) r).setPassword("****");
					}
					response.getWriter().println(r.toXml());
					r = null;
				}
			}
		} catch (final Exception e) {
			getLogger().exception(e);
			mileStone("<error method='writeRows' msg='" + e.getMessage() + "' />");
			getLogger().error(e.toString());
			throw new IOException(e.toString());
		} finally {
			rows = null;
			rowsEnum = null;
			response.setStatus(HttpServletResponse.SC_OK);
			mileStone("</writeRows>");
			notifyAll();
		}
	}

	/**
	 * This write file content to output stream
	 *
	 * @param f
	 *            is the file to write
	 */
	@Override
	public synchronized void writeFile(final File f) throws IOException {
		try {
			mileStone("<writeFile file='" + f + "'>");
			//
			// 2 dec 2007 : we force nio to false
			//
			final StreamIO io = new StreamIO(new DataOutputStream(response.getOutputStream()), null, false);
			io.writeFileContent(f);
		} catch (final Exception e) {
			getLogger().exception(e);
			mileStone("<error method='writeFile' msg='" + e.getMessage() + "' />");
			throw new IOException(e.toString());
		} finally {
			mileStone("</writeFile>");
			notifyAll();
		}
	}

	/**
	 * This is not implemented and always throws an IOException
	 */
	@Override
	public void readFile(final File f) throws IOException {
		throw new IOException("HTTPHandler#readFile not implemented");
	}

	/**
	 * This handles incoming connections. This is inherited from
	 * org.mortbay.jetty.Handler. This expects a POST parameter :
	 * XWPostParams.COMMAND
	 *
	 * @see xtremweb.communications.XWPostParams
	 */
	@Override
	public synchronized void handle(final String target, final Request baseRequest, final HttpServletRequest _request,
			final HttpServletResponse _response) throws IOException, ServletException {

		final boolean answered = false;
		XMLRPCCommand command = null;
		final Logger logger = getLogger();
		Table obj = null;
		dataUpload = null;

		request = _request;
		response = _response;

		String reqUri = baseRequest.getRequestURI().toString();

		if (request.getUserPrincipal() == null) {
			logger.debug("Handling user principal = null");
		} else {
			logger.debug("Handling user principal = " + request.getUserPrincipal().getName());
		}
		logger.debug("Handling target         = " + target);
		logger.finest("Handling request        = " + request.getContentLength() + " " + request.getContentType());
		logger.debug("Handling request auth   = " + request.getAuthType());
		logger.debug("Handling request user   = " + request.getRemoteUser());
		logger.debug("Handling parameter size = " + request.getParameterMap().size());
		logger.debug("Handling query string   = " + request.getQueryString());
		logger.debug("Handling method         = " + request.getMethod());
		logger.debug("Request URI             = " + reqUri);
		// logger.debug("Request server = " + request.getServerName());
		// logger.debug("Request port = " + request.getServerPort());
		// logger.debug("Authorization = " +
		// request.getHeader(HttpHeaders.AUTHORIZATION));
		for (final Enumeration<String> e = request.getParameterNames(); e.hasMoreElements();) {
			logger.finest("parameter " + e.nextElement());
		}
		for (final Enumeration<String> e = request.getHeaderNames(); e.hasMoreElements();) {
			logger.finest("header " + e.nextElement());
		}

		if (request.getParameterMap().size() <= 0) {
			/*
			 * if(target.equals(PATH) == true){ redirectPage(baseRequest,
			 * Resources.XWHTML); return; }
			 */
			for (final Resources r : Resources.values()) {
				if (r.getName().compareToIgnoreCase(target) == 0) {
					logger.debug("Sending resource " + r.getName());
					sendResource(r);
					return;
				}
			}
		}

		UserInterface user = null;

		user = userFromCertificate(request);
		if (user == null) {
			user = userFromOpenId(request);
		}
		if (user == null) {
			user = userFromOAuth(request);
		}
		if (user == null) {
			user = userFromPostParams(request);
		}

		final Vector<String> paths = (Vector<String>) XWTools.split(target, "/");
		logger.debug("paths.size() = " + paths.size());

		URI baseUri = null;
		try {
			baseUri = new URI(Connection.httpsScheme() + "://" + XWTools.getHostName(request.getServerName()) + ":"
					+ request.getServerPort());
		} catch (final URISyntaxException e) {
			resetIdRpc();
			throw new IOException(e.getMessage());
		}

		if (target.compareToIgnoreCase(APIPATH) == 0) {
			resetIdRpc();
			writeApi(user, baseUri);
			baseRequest.setHandled(true);
			return;
		}

		if (target.equals(PATH) == false) {
			try {
				final IdRpc i = IdRpc.valueOf(paths.elementAt(0).toUpperCase());
				waitIdRpc(i);
			} catch (final Exception e) {
				// let other handlers manage this (e.g. /stats, /openid)
				logger.debug("ignoring " + reqUri);
				return;
			}
		}

		if (user == null) {
			resetIdRpc();
			logger.debug("no credential found");
			redirectPage(baseRequest, Resources.LOGINPAGE);
			return;
		}

		try {

			final boolean isMultipart = ServletFileUpload.isMultipartContent(request);

			logger.debug("Handling ismultipart    = " + isMultipart);

			//
			// HTML form
			//
			if (isMultipart) {
				final List parts = servletUpload.parseRequest(request);
				if (parts != null) {
					for (final Iterator it = parts.iterator(); it.hasNext();) {
						final FileItem item = (FileItem) it.next();

						logger.debug("multipart item = " + item.getFieldName().toUpperCase());

						try {
							switch (XWPostParams.valueOf(item.getFieldName().toUpperCase())) {
							case DATAUID:
								reqUri += "/" + item.getString();
								break;
							case DATAFILE:
								dataUpload = item;
								break;
							case DATASIZE:
								dataUploadSize = Long.parseLong(item.getString());
								break;
							case DATAMD5SUM:
								dataUploadmd5sum = item.getString();
								break;
							}
						} catch (final Exception e) {
							logger.exception(e);
						}
					}
				}
			}

			for (final Enumeration<String> e = request.getParameterNames(); e.hasMoreElements();) {
				logger.debug("parameter " + e.nextElement());
			}
			for (final Enumeration<String> e = request.getHeaderNames(); e.hasMoreElements();) {
				logger.debug("header " + e.nextElement());
			}

			if (target.equals(PATH)) {
				usagePage(user);
				baseRequest.setHandled(true);
				return;
			}

			setRemoteName(request.getRemoteHost());
			setRemoteIP(request.getRemoteAddr());
			setRemotePort(request.getRemotePort());

			if (dataUpload != null) {
				String value = request.getParameter(XWPostParams.DATASIZE.toString());
				if (value != null) {
					logger.debug("Parsing parameters DATASIZE = " + value + " (" + dataUploadSize + ")");
					if (dataUploadSize == -1) {
						dataUploadSize = Long.parseLong(value);
					}
				}
				value = request.getParameter(XWPostParams.DATAMD5SUM.toString());
				if (value != null) {
					logger.debug("Parsing parameters DATAMD5SUM = " + value + " (" + dataUploadmd5sum + ")");
					if (dataUploadmd5sum == null) {
						dataUploadmd5sum = value;
					}
				}
			}

			logger.debug("Parameters dataUploadSize = " + dataUploadSize + " dataUploadmd5sum = " + dataUploadmd5sum);

			final Iterator<String> it = paths.iterator();
			String uriWithoutCmd = new String();
			int i = 0;
			while (it.hasNext()) {
				final String st = it.next();
				logger.debug("Parsing path path = " + st);
				if (i++ > 0) {
					uriWithoutCmd += "/" + st;
				}
			}

			final URI uri = new URI(Connection.httpsScheme() + "://" + XWTools.getHostName(request.getServerName())
					+ ":" + request.getServerPort() + uriWithoutCmd);
			uriWithoutCmd = null;
			logger.debug("URI = " + uri);

			final String objXmlDesc = request.getParameter(XWPostParams.XMLDESC.toString());
			logger.debug("objXmlDesc = " + objXmlDesc);

			if (objXmlDesc != null) {
				final ByteArrayInputStream in = new ByteArrayInputStream(objXmlDesc.getBytes());
				try {
					obj = Table.newInterface(in);
				} finally {
					in.close();
				}
			}

			if (obj != null) {
				logger.debug("obj = " + obj.toXml());
				if (obj.getUID() == null) {
					obj.setUID(new UID());
				}
			} else {
				logger.debug("obj = null");
			}
			command = getIdRpc().newCommand(uri, user, obj);

			final String parameter = request.getParameter(XWPostParams.PARAMETER.toString());
			logger.debug("parameter = " + parameter);

			if (parameter != null) {
				switch (getIdRpc()) {
				case CHMOD: {
					final XWAccessRights ar = new XWAccessRights(parameter);
					((XMLRPCCommandChmod) command).setModifier(ar);
					break;
				}
				case ACTIVATEHOST: {
					final Boolean b = new Boolean(parameter);
					((XMLRPCCommandActivateHost) command).setActivation(b);
					break;
				}
				case WORKALIVE: {
					final XMLHashtable p = new XMLHashtable(StreamIO.stream(parameter));
					logger.debug("XMLHastable = " + (p == null ? "null" : p.toXml()));
					((XMLRPCCommandWorkAlive) command).setParameter(p);
					break;
				}
				default:
					break;
				}
			}
			command.getUser().setLogin(user.getLogin());
			command.getUser().setPassword(user.getPassword());
			command.getUser().setUID(user.getUID());
		} catch (final Exception e) {
			command = null;
			logger.exception(e);
			logger.error(e.toString());
		}

		try {
			if (command != null) {
				logger.debug("cmd = " + command.toXml());

				if (command.getIdRpc() == IdRpc.DOWNLOADDATA) {
					final UID uid = command.getURI().getUID();
					String contentTypeValue = CONTENTTYPEVALUE;

					try {
						final DataInterface theData = DBInterface.getInstance().getData(command.getUser(), uid);
						final DataTypeEnum dataType = theData != null ? theData.getType() : null;
						final Date lastModified = theData != null ? theData.getMTime() : null;
						if (theData != null) {
							if (lastModified != null) {
								response.setHeader(LASTMODIFIEDLABEL, lastModified.toString());
							}
							response.setHeader(CONTENTLENGTHLABEL, "" + theData.getSize());
							response.setHeader(CONTENTMD5LABEL, theData.getMD5());
							response.setHeader(CONTENTDISPOSITIONLABEL,
									"attachment; filename=\"" + theData.getName() + "\"");
						}
						if (dataType != null) {
							contentTypeValue = dataType.getMimeType();
						}
					} catch (final Exception e) {
						if (logger.debug()) {
							logger.exception(e);
						}
					}

					logger.debug("set " + CONTENTTYPELABEL + " : " + contentTypeValue);
					response.setHeader(CONTENTTYPELABEL, contentTypeValue);
				}

				super.run(command);
			} else {
				if (answered == false) {
					write((XMLable) null);
				}
			}
		} finally {
			command = null;
			user = null;
			obj = null;
			baseRequest.setHandled(true);
			notifyAll();
		}
	}

	/**
	 * This retrieves the user from its X509 certificate
	 *
	 * @throws IOException
	 */
	private UserInterface userFromCertificate(final HttpServletRequest request) throws IOException {

		final Object certChain = request.getAttribute("javax.servlet.request.X509Certificate");

		if (certChain == null) {
			return null;
		}

		final Logger logger = getLogger();

		final UserInterface client = new UserInterface();

		final X509Certificate certs[] = (X509Certificate[]) certChain;
		int i = 0;
		for (final X509Certificate cert : certs) {
			final String dn = cert.getSubjectDN().getName();
			logger.debug("SubjectDN[" + i++ + "] = " + dn);
			try {
				final String dnu = dn.toUpperCase();
				final int idx = dnu.indexOf(DNHEADER_EMAIL);
				if (idx != -1) {
					final int startemail = idx + DNHEADERLENGTH_EMAIL;
					int endemail = dnu.indexOf(',', startemail);
					if (endemail == -1) {
						endemail = dnu.length() - startemail;
					}
					final String email = dn.substring(startemail, endemail);
					logger.debug("email = " + email);
					if (email != null) {
						client.setEMail(email);
					}
				}
			} catch (final Exception e) {
			}
		}
		final String subjectName = certs[0].getSubjectX500Principal().getName();
		final String issuerName = certs[0].getIssuerX500Principal().getName();
		final String loginName = subjectName + "_" + issuerName;
		final String random = loginName + Math.random() + System.currentTimeMillis();
		final byte[] strb = random.getBytes();
		final MD5 md5 = new MD5(strb);
		final String md5hex = md5.asHex();
		client.setLogin(loginName); // login may be truncated; see
		// UserIntergace.USERLOGINLENGTH

		final UserInterface ret = DBInterface.getInstance()
				.user(UserInterface.Columns.LOGIN.toString() + "= '" + client.getLogin() + "'");

		if (ret != null) {
			ret.setEMail(client.getEMail());
			logger.debug(("user = " + ret) == null ? "null" : ret.toXml());
			return ret;
		}

		final UserInterface admin = Dispatcher.getConfig().getProperty(XWPropertyDefs.ADMINLOGIN) == null ? null
				: DBInterface.getInstance()
						.user(SQLRequest.MAINTABLEALIAS + "." + UserInterface.Columns.LOGIN.toString() + "='"
								+ Dispatcher.getConfig().getProperty(XWPropertyDefs.ADMINLOGIN) + "'");
		if (admin == null) {
			throw new IOException("can't insert new certified user");
		}

		client.setUID(new UID());
		client.setLogin(loginName);
		client.setPassword(md5hex);
		if (client.getEMail() == null) {
			client.setEMail(loginName);
		}
		client.setOwner(Dispatcher.getConfig().getAdminUid());
		client.setRights(UserRightEnum.STANDARD_USER);

		try {
			DBInterface.getInstance().addUser(admin, client);
			return client;
		} catch (final Exception e) {
			throw new IOException("user certification error : " + e.getMessage());
		}
	}

	/**
	 * This retrieves the user from a remove OpenId server
	 */
	private UserInterface userFromOpenId(final HttpServletRequest request) throws IOException {

		final HttpSession session = request.getSession(true);
		/*
		 * String nonce =
		 * request.getParameter(XWPostParams.AUTH_NONCE.toString()); String
		 * email = request.getParameter(XWPostParams.AUTH_EMAIL.toString());
		 * String id =
		 * request.getParameter(XWPostParams.AUTH_IDENTITY.toString());
		 *
		 * if(nonce == null) { nonce = (String)
		 * session.getAttribute(XWPostParams.AUTH_NONCE.toString()); } if(email
		 * == null) { email = (String)
		 * session.getAttribute(XWPostParams.AUTH_EMAIL.toString()); } if(id ==
		 * null) { id = (String)
		 * session.getAttribute(XWPostParams.AUTH_IDENTITY.toString()); }
		 *
		 */
		final String authNonce = request.getParameter(XWPostParams.AUTH_NONCE.toString()) != null
				? request.getParameter(XWPostParams.AUTH_NONCE.toString())
				: (String) session.getAttribute(XWPostParams.AUTH_NONCE.toString());
		final String authEmail = request.getParameter(XWPostParams.AUTH_EMAIL.toString()) != null
				? request.getParameter(XWPostParams.AUTH_EMAIL.toString())
				: (String) session.getAttribute(XWPostParams.AUTH_EMAIL.toString());
		final String authId = request.getParameter(XWPostParams.AUTH_IDENTITY.toString()) != null
				? request.getParameter(XWPostParams.AUTH_IDENTITY.toString())
				: (String) session.getAttribute(XWPostParams.AUTH_IDENTITY.toString());

		if ((authNonce == null) || (authEmail == null)) {
			return null;
		}

		UserInterface ret = null;

		try {
			HTTPOpenIdHandler.getInstance().verifyNonce(authNonce);
			ret = DBInterface.getInstance().user(UserInterface.Columns.EMAIL.toString() + "= '" + authEmail + "'");
			if (ret == null) {
				if (Dispatcher.getConfig().getBoolean(XWPropertyDefs.DELEGATEDREGISTRATION) == false) {
					throw new IOException("delegated registration is not allowed");
				}

				final UserInterface admin = Dispatcher.getConfig().getProperty(XWPropertyDefs.ADMINLOGIN) == null ? null
						: DBInterface.getInstance()
								.user(SQLRequest.MAINTABLEALIAS + "." + UserInterface.Columns.LOGIN.toString() + "='"
										+ Dispatcher.getConfig().getProperty(XWPropertyDefs.ADMINLOGIN) + "'");
				if (admin == null) {
					throw new IOException("can't insert new OpenId user (cant't retrieve admin)");
				}
				final String random = authEmail + System.currentTimeMillis() + Math.random();
				final byte[] strb = random.getBytes();
				final MD5 md5 = new MD5(strb);
				final String md5hex = md5.asHex();
				final UserInterface client = new UserInterface();
				client.setUID(new UID());
				client.setOwner(Dispatcher.getConfig().getAdminUid());
				client.setLogin(authId);
				client.setPassword(md5hex);
				client.setRights(UserRightEnum.STANDARD_USER);
				client.setEMail(authEmail);
				DBInterface.getInstance().addUser(admin, client);
				ret = client;
			}
			session.setAttribute(XWPostParams.AUTH_NONCE.toString(), authNonce);
			session.setAttribute(XWPostParams.AUTH_EMAIL.toString(), authEmail);
		} catch (final Exception e) {
			throw new IOException("openid delegation error : " + e.getMessage());
		}

		return ret;
	}

	/**
	 * This retrieves the user from a remove OAuth server
	 */
	private UserInterface userFromOAuth(final HttpServletRequest request) throws IOException {
		final UserInterface admin = Dispatcher.getConfig().getProperty(XWPropertyDefs.ADMINLOGIN) == null ? null
				: DBInterface.getInstance()
						.user(SQLRequest.MAINTABLEALIAS + "." + UserInterface.Columns.LOGIN.toString() + "='"
								+ Dispatcher.getConfig().getProperty(XWPropertyDefs.ADMINLOGIN) + "'");

		final HttpSession session = request.getSession(true);
		final String authNonce = request.getParameter(XWPostParams.AUTH_NONCE.toString()) != null
				? request.getParameter(XWPostParams.AUTH_NONCE.toString())
				: (String) session.getAttribute(XWPostParams.AUTH_NONCE.toString());
		final String authState = request.getParameter(XWPostParams.AUTH_STATE.toString()) != null
				? request.getParameter(XWPostParams.AUTH_STATE.toString())
				: (String) session.getAttribute(XWPostParams.AUTH_STATE.toString());
		final String authEmail = request.getParameter(XWPostParams.AUTH_EMAIL.toString()) != null
				? request.getParameter(XWPostParams.AUTH_EMAIL.toString())
				: (String) session.getAttribute(XWPostParams.AUTH_EMAIL.toString());
		final String authId = request.getParameter(XWPostParams.AUTH_IDENTITY.toString()) != null
				? request.getParameter(XWPostParams.AUTH_IDENTITY.toString())
				: (String) session.getAttribute(XWPostParams.AUTH_IDENTITY.toString());

		if ((authState == null) || (authEmail == null)) {
			return null;
		}

		UserInterface ret = null;
		try {
			HTTPOpenIdHandler.getInstance().verifyNonce(authNonce);
			ret = DBInterface.getInstance().user(UserInterface.Columns.EMAIL.toString() + "= '" + authEmail + "'");
			if (ret == null) {
				if (Dispatcher.getConfig().getBoolean(XWPropertyDefs.DELEGATEDREGISTRATION) == false) {
					throw new IOException("unaccepted email address : " + authEmail);
				} else {
					if (admin == null) {
						throw new IOException("can't insert new OpenId user");
					}
					final String random = authEmail + System.currentTimeMillis() + Math.random();
					final byte[] strb = random.getBytes();
					final MD5 md5 = new MD5(strb);
					final String md5hex = md5.asHex();
					final UserInterface client = new UserInterface();
					client.setUID(new UID());
					client.setOwner(Dispatcher.getConfig().getAdminUid());
					client.setLogin(authId);
					client.setPassword(md5hex);
					client.setRights(UserRightEnum.STANDARD_USER);
					client.setEMail(authEmail);
					DBInterface.getInstance().addUser(admin, client);
					ret = client;
				}
			}
			session.setAttribute(XWPostParams.AUTH_NONCE.toString(), authNonce);
			session.setAttribute(XWPostParams.AUTH_EMAIL.toString(), authEmail);
		} catch (final Exception e) {
			throw new IOException("openid delegation error : " + e.getMessage());
		}

		return ret;
	}

	/**
	 * This retrieves the user from login/password
	 *
	 * @since 10.2.0
	 */
	private UserInterface userFromPostParams(final HttpServletRequest request) throws IOException {

		final HttpSession session = request.getSession(true);
		final String login = (request.getParameter(XWPostParams.XWLOGIN.toString()) != null
				? request.getParameter(XWPostParams.XWLOGIN.toString())
				: (String) session.getAttribute(XWPostParams.XWLOGIN.toString()));
		final String passwd = (request.getParameter(XWPostParams.XWPASSWD.toString()) != null
				? request.getParameter(XWPostParams.XWPASSWD.toString())
				: (String) session.getAttribute(XWPostParams.XWPASSWD.toString()));

		if ((login == null) || (passwd == null)) {
			return null;
		}

		try {
			final UserInterface ret = DBInterface.getInstance()
					.user(SQLRequest.MAINTABLEALIAS + "." + UserInterface.Columns.LOGIN.toString() + "='" + login
							+ "' AND " + SQLRequest.MAINTABLEALIAS + "." + UserInterface.Columns.PASSWORD.toString()
							+ "='" + passwd + "'");

			session.setAttribute(XWPostParams.XWLOGIN.toString(), login);
			session.setAttribute(XWPostParams.XWPASSWD.toString(), passwd);
			return ret;
		} catch (final Exception e) {
			throw new IOException("userFromPostParams error : " + e.getMessage());
		}
	}

	/**
	 * This uploads a data to server<br />
	 * Data must be defined on server side (i.e. sendData() must be called
	 * first)
	 *
	 * @param client
	 *            is the caller attributes
	 * @param uid
	 *            is the UID of the data to upload
	 * @see #sendData(UserInterface, DataInterface)
	 */
	@Override
	public synchronized long uploadData(final UserInterface client, final UID uid)
			throws IOException, InvalidKeyException, AccessControlException {

		final DataInterface theData = DBInterface.getInstance().getData(client, uid);
		long ret = 0;

		mileStone("<uploadData>");

		try {
			if (theData == null) {
				mileStone("<error method='uploadData' msg='no data' />");
				mileStone("</uploadData>");
				notifyAll();
				throw new RemoteException("uploadData(" + uid + ") data not found");
			}

			final File dFile = theData.getPath();

			if (dataUpload == null) {
				mileStone("<error method='uploadData' msg='no data upload' />");
				mileStone("</uploadData>");
				notifyAll();
				throw new IOException("upload is null");
			}

			dataUpload.write(dFile);
			final long fsize = dFile.length();
			final String fmd5sum = MD5.asHex(MD5.getHash(dFile));
			if (fsize != dataUploadSize) {
				dFile.delete();
				throw new IOException("Upload file size error should be " + dataUploadSize + " but found " + fsize);
			}
			if ((dataUploadmd5sum == null) || (dataUploadmd5sum.compareToIgnoreCase(fmd5sum) != 0)) {
				dFile.delete();
				throw new IOException(
						"Upload file MD5sum error should be " + dataUploadmd5sum + " but found " + fmd5sum);
			}
			theData.setSize(fsize);
			theData.setMD5(fmd5sum);
			theData.setStatus(StatusEnum.AVAILABLE);
			theData.update();
			ret = dFile.length();
		} catch (final InvalidKeyException e) {
			mileStone("<error method='uploadData' msg='" + e.getMessage() + "' />");
			throw e;
		} catch (final AccessControlException e) {
			mileStone("<error method='uploadData' msg='" + e.getMessage() + "' />");
			throw e;
		} catch (final Exception e) {
			try {
				theData.setStatus(StatusEnum.ERROR);
				theData.update();
			} catch (final Exception e2) {
				getLogger().exception(e2);
			}

			getLogger().exception(e);
			mileStone("<error method='uploadData' msg='" + e.getMessage() + "' />");
			throw new RemoteException(e.toString());
		} finally {
			dataUpload = null;
			dataUploadSize = -1;
			dataUploadmd5sum = null;
			mileStone("</uploadData>");
			notifyAll();
		}
		return ret;
	}

	/**
	 * This cleans and closes communications
	 */
	@Override
	public void close() {

		mileStone("<close>");

		try {
			request.getInputStream().close();
		} catch (final Exception e) {
		}

		try {
			response.getWriter().flush();
		} catch (final Exception e) {
		}

		try {
			response.getWriter().close();
		} catch (final Exception e) {
		}

		mileStone("</close>");
	}
}
