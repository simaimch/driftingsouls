/*
 *	Drifting Souls 2
 *	Copyright (c) 2006 Christopher Jung
 *
 *	This library is free software; you can redistribute it and/or
 *	modify it under the terms of the GNU Lesser General Public
 *	License as published by the Free Software Foundation; either
 *	version 2.1 of the License, or (at your option) any later version.
 *
 *	This library is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *	Lesser General Public License for more details.
 *
 *	You should have received a copy of the GNU Lesser General Public
 *	License along with this library; if not, write to the Free Software
 *	Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
package net.driftingsouls.ds2.server.framework.pipeline;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.apache.commons.fileupload.FileItem;

/**
 * Standardinterface fuer Requestdaten.
 * @author Christopher Jung
 *
 */
public interface Request {
	/**
	 * Gibt einen Parameter des Aufrufs zurueck.
	 * Falls der Parameter nicht angegeben wurde wird <code>null</code> zurueckgegeben
	 * @param parameter Der Name des Parameters
	 * @return Der Wert oder <code>null</code>
	 */
	public String getParameter(String parameter);
	
	/**
	 * Gibt einen Parameter als String zurueck. Sollte der Parameter
	 * nicht angegeben worden sein wird ein leerer String zurueckgegeben.
	 * @param parameter Der Parameter
	 * @return Der Wert oder ein leerer String
	 */
	public String getParameterString(String parameter);
	
	/**
	 * Gibt einen Parameter als Zahl zurueck. Sollte der Parameter nicht
	 * angegeben worden sein oder sollte er keine Zahl sein wird <code>0</code>
	 * zurueckgegeben.
	 * @param parameter Der Parameter
	 * @return Der Wert oder <code>0</code>
	 */
	public int getParameterInt(String parameter);
	
	/**
	 * Setzt einen Parameter auf einen bestimmten Wert.
	 * @param parameter Der Parameter
	 * @param value Der neue Wert
	 */
	public void setParameter(String parameter, String value);
	
	/**
	 * Gibt den ContentType des Aufrufs zurueck.
	 * @return Der ContentType
	 */
	public String getContentType();
	
	/**
	 * Gibt den InputStream des Aufrufs zurueck.
	 * @return Der InputStream
	 * @throws IOException
	 */
	public InputStream getInputStream() throws IOException;
	
	/**
	 * Gibt den Query-String zurueck.
	 * @return Der Query-String
	 */
	public String getQueryString();
	
	/**
	 * Gibt den Aufrufpfad zurueck.
	 * @return Der Aufrufpfad
	 */
	public String getPath();
	
	/**
	 * Gibt die im Aufruf verwendete Encoding zurueck.
	 * @return Das Charset
	 */
	public String getCharacterEncoding();
	
	/**
	 * Gibt die Anzahl an Bytes des Aufrufs zurueck.
	 * @return Die Laenge
	 */
	public int getContentLength();
	
	/**
	 * Gibt den Header mit dem angegebenen Namen zurueck.
	 * Sollte der Header nicht existieren wird <code>null</code> zurueckgegeben.
	 * @param header Der Header
	 * @return Der Wert oder <code>null</code>
	 */
	public String getHeader(String header);
	
	/**
	 * Gibt die Adresse des Aufrufers zurueck.
	 * @return Die Adresse des Aufrufers
	 */
	public String getRemoteAddress();
	
	/**
	 * Gibt die fuer die Anfrage verwendete URL zurueck.
	 * @return Die URL
	 */
	public String getRequestURL();
	
	/**
	 * Gibt den User-Agent des Aufrufers zurueck.
	 * @return Der User-Agent
	 */
	public String getUserAgent();
	
	/**
	 * Gibt evt hochgeladene Dateien zurueck.
	 * @return Die Liste der hochgeladenen Dateien
	 */
	public List<FileItem> getUploadedFiles();
	
	/**
	 * Liefert eine pro Session einmalige Instanz einer Klasse.
	 * Sollte keine Instanz dieser Klasse in der Session vorhanden sein,
	 * wird dieses erstellt.
	 * 
	 * @param <T> Eine Klasse, welche Session arbeiten kann
	 * @param cls Die gewuenschte Klasse
	 * @return Eine Instanz der Klase
	 */
	public <T> T getFromSession(Class<T> cls);
	
	/**
	 * Entfernt die Instanz dieser Klasse aus der Session.
	 * @param cls Die Klasse
	 */
	public void removeFromSession(Class<?> cls);
	
	/**
	 * Gibt einen Cookie zurueck.
	 * 
	 * @param name Name des Cookies.
	 * @return Wert des Cookie oder <code>null</code>, wenn er nicht existiert.
	 */
	public String getCookie(String name);

	/**
	 * Gibt eine Map mit allen Parametern der Request zurueck.
	 * Key ist der Parametername.
	 * @return Die Map
	 */
	public Map<String,String> getParameterMap();
}
