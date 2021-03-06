/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2012 "Artur Hefczyc" <artur.hefczyc@tigase.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. Look for COPYING file in the top folder.
 * If not, see http://www.gnu.org/licenses/.
 *
 * $Rev$
 * Last modified by $Author$
 * $Date$
 */

package tigase.xmpp.impl;

//~--- non-JDK imports --------------------------------------------------------

import tigase.db.NonAuthUserRepository;

import tigase.server.Command;
import tigase.server.Packet;

import tigase.xml.Element;

import tigase.xmpp.StanzaType;
import tigase.xmpp.XMPPException;
import tigase.xmpp.XMPPProcessor;
import tigase.xmpp.XMPPProcessorIfc;
import tigase.xmpp.XMPPResourceConnection;

//~--- JDK imports ------------------------------------------------------------

import java.util.Map;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;

//~--- classes ----------------------------------------------------------------

/**
 * Created: Jul 29, 2009 4:03:44 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class StartZLib extends XMPPProcessor implements XMPPProcessorIfc {
	private static Logger log = Logger.getLogger(StartZLib.class.getName());
	private static final String XMLNS = "http://jabber.org/protocol/compress";
	private static final String ID = "zlib";
	private static final String[] ELEMENTS = { "compress", "compressed", "failure" };
	private static final String[] XMLNSS = { XMLNS, XMLNS, XMLNS };
	private static final Element[] FEATURES = {
		new Element("compression", new Element[] { new Element("method", "zlib") },
			new String[] { "xmlns" }, new String[] { "http://jabber.org/features/compress" }) };

	//~--- fields ---------------------------------------------------------------

	private Element compressed = new Element("compressed", new String[] { "xmlns" },
		new String[] { XMLNS });
	private Element failure = new Element("failure", new String[] { "xmlns" },
		new String[] { XMLNS });

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	public String id() {
		return ID;
	}

	/**
	 * Method description
	 *
	 *
	 * @param packet
	 * @param session
	 * @param repo
	 * @param results
	 * @param settings
	 *
	 * @throws XMPPException
	 */
	@Override
	public void process(Packet packet, XMPPResourceConnection session, NonAuthUserRepository repo,
			Queue<Packet> results, Map<String, Object> settings)
			throws XMPPException {
		if (session == null) {
			return;
		}    // end of if (session == null)

		if (packet.isElement("compress", XMLNS)) {
			if (session.getSessionData(ID) != null) {

				// Somebody tries to activate multiple TLS layers.
				// This is possible and can even work but this can also be
				// a DOS attack. Blocking it now, unless someone requests he wants
				// to have multiple layers of TLS for his connection
				log.log(Level.WARNING,
						"Multiple ZLib requests, possible DOS attack, closing connection: {0}", packet);
				results.offer(packet.swapFromTo(failure, null, null));
				results.offer(Command.CLOSE.getPacket(packet.getTo(), packet.getFrom(), StanzaType.set,
						session.nextStanzaId()));

				return;
			}

			session.putSessionData(ID, "true");

			Packet result = Command.STARTZLIB.getPacket(packet.getTo(), packet.getFrom(), StanzaType.set,
				session.nextStanzaId(), Command.DataType.submit);

			Command.setData(result, compressed);
			results.offer(result);
		} else {
			log.log(Level.WARNING, "Unknown ZLIB element: {0}", packet);
			results.offer(packet.swapFromTo(failure, null, null));
			results.offer(Command.CLOSE.getPacket(packet.getTo(), packet.getFrom(), StanzaType.set,
					session.nextStanzaId()));
		}
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	public String[] supElements() {
		return ELEMENTS;
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	public String[] supNamespaces() {
		return XMLNSS;
	}

	/**
	 * Method description
	 *
	 *
	 * @param session
	 *
	 * @return
	 */
	@Override
	public Element[] supStreamFeatures(final XMPPResourceConnection session) {

		// If session does not exist, just return null, we don't provide features
		// for non-existen stream
		// We also do not want to provide compression if it is already started
		// and the compression has to be available after TLS has been completed.
		if ((session != null) && (session.getSessionData(ID) == null)) {

//    && session.getSessionData(StartTLS.ID) != null) {
			return FEATURES;
		} else {
			return null;
		}    // end of if (session.isAuthorized()) else
	}
}


//~ Formatted in Sun Code Convention


//~ Formatted by Jindent --- http://www.jindent.com
