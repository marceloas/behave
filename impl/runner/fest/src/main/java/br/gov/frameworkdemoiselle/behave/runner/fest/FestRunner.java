/*
 * Demoiselle Framework
 * Copyright (C) 2013 SERPRO
 * ----------------------------------------------------------------------------
 * This file is part of Demoiselle Framework.
 * 
 * Demoiselle Framework is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License version 3
 * as published by the Free Software Foundation.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License version 3
 * along with this program; if not,  see <http://www.gnu.org/licenses/>
 * or write to the Free Software Foundation, Inc., 51 Franklin Street,
 * Fifth Floor, Boston, MA  02110-1301, USA.
 * ----------------------------------------------------------------------------
 * Este arquivo é parte do Framework Demoiselle.
 * 
 * O Framework Demoiselle é um software livre; você pode redistribuí-lo e/ou
 * modificá-lo dentro dos termos da GNU LGPL versão 3 como publicada pela Fundação
 * do Software Livre (FSF).
 * 
 * Este programa é distribuído na esperança que possa ser útil, mas SEM NENHUMA
 * GARANTIA; sem uma garantia implícita de ADEQUAÇÃO a qualquer MERCADO ou
 * APLICAÇÃO EM PARTICULAR. Veja a Licença Pública Geral GNU/LGPL em português
 * para maiores detalhes.
 * 
 * Você deve ter recebido uma cópia da GNU LGPL versão 3, sob o título
 * "LICENCA.txt", junto com esse programa. Se não, acesse <http://www.gnu.org/licenses/>
 * ou escreva para a Fundação do Software Livre (FSF) Inc.,
 * 51 Franklin St, Fifth Floor, Boston, MA 02111-1301, USA.
 */
package br.gov.frameworkdemoiselle.behave.runner.fest;

import java.awt.Container;
import java.awt.Window;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;

import org.apache.log4j.Logger;
import org.fest.swing.core.BasicComponentPrinter;
import org.fest.swing.core.ComponentPrinter;
import org.fest.swing.core.Robot;
import org.fest.swing.edt.GuiActionRunner;
import org.fest.swing.edt.GuiQuery;
import org.fest.swing.fixture.FrameFixture;
import org.fest.swing.image.ScreenshotTaker;

import br.gov.frameworkdemoiselle.behave.annotation.ElementMap;
import br.gov.frameworkdemoiselle.behave.config.BehaveConfig;
import br.gov.frameworkdemoiselle.behave.exception.BehaveException;
import br.gov.frameworkdemoiselle.behave.internal.spi.InjectionManager;
import br.gov.frameworkdemoiselle.behave.internal.util.ReflectionUtil;
import br.gov.frameworkdemoiselle.behave.message.BehaveMessage;
import br.gov.frameworkdemoiselle.behave.runner.Runner;
import br.gov.frameworkdemoiselle.behave.runner.fest.annotation.ElementIndex;
import br.gov.frameworkdemoiselle.behave.runner.fest.util.DesktopElement;
import br.gov.frameworkdemoiselle.behave.runner.fest.util.DesktopReflectionUtil;
import br.gov.frameworkdemoiselle.behave.runner.ui.Element;
import br.gov.frameworkdemoiselle.behave.runner.ui.Screen;

public class FestRunner implements Runner {

	
	public static String MESSAGEBUNDLE = "demoiselle-runner-fest-bundle";
	private Logger logger = Logger.getLogger(this.toString());
	
	private BehaveMessage message = new BehaveMessage(MESSAGEBUNDLE);

	public Robot robot;
	public JFrame mainFrame;
	public Container mainContainer;
	public Container currentContainer;
	public FrameFixture mainFixture;
	// public JComponentFixture currentFixture;
	public String currentTitle;

	@Override
	public void start() {
		
		logger.info(message.getString("message-fest-started"));

		if (mainFrame == null) {			
			mainFrame = getInstance(true);
			if (mainFrame == null){
				mainFrame =getInstance(false);
			}
			if (mainFrame == null){
				throw new BehaveException("Propriedades behave.runner.app.mainClass e behave.runner.app.startupFrame não informadas no behave.properties");
			}
			JFrame frame = GuiActionRunner.execute(new GuiQuery<JFrame>() {
				protected JFrame executeInEDT() {
					return mainFrame;			
				}
			});
			mainContainer = frame;
			mainContainer.setVisible(true);

			mainFixture = new FrameFixture((JFrame) mainContainer);
			robot = mainFixture.robot;
		}
		currentContainer = mainContainer;
	}
	
	private JFrame getInstance(boolean mainClass) {
		String clazz = (mainClass) ? BehaveConfig.getProperty("behave.runner.app.mainClass") : BehaveConfig.getProperty("behave.runner.app.startupFrame");
		logger.info(message.getString("message-fest-started"));
		try {						
			if (clazz == null || clazz.equals("")){
				return null;
			}
			logger.info(message.getString("message-fest-class", clazz));
			Object instance =  Class.forName(clazz).newInstance();
			if (mainClass){
				return (JFrame) instance;	
			}else{
				return ((FestStartup) instance).getFrame();
			}		
		}  catch (InstantiationException e) {
			throw new BehaveException(e);
		} catch (IllegalAccessException e) {
			throw new BehaveException(e);
		} catch (ClassNotFoundException e) {
			throw new BehaveException(message.getString("exception-main-class-not-found", clazz), e);
		} catch (ClassCastException e) {			
			throw new BehaveException(e);
		} 
		
	}

	

	@Override
	public void get(String url) {

	}

	@Override
	public void navigateTo(String title) {
		// Procura por Dialogs
		for (Window w : JDialog.getWindows()) {
			if (w instanceof JDialog && w.isVisible()) {
				if (title.trim().equalsIgnoreCase(((JDialog) w).getTitle().trim())) {
					currentContainer = (JPanel) ((JDialog) w).getRootPane().getContentPane();
					currentContainer.setFocusTraversalKeysEnabled(true);
					currentContainer.setVisible(true);
					currentContainer.setEnabled(true);

					currentTitle = title;

					logger.debug("Navigate To (Dialog): " + title);

					return;
				}
			}
		}

		// Procura por Frames
		for (Window w : JFrame.getWindows()) {
			if (w instanceof JFrame && w.isVisible()) {

				JFrame frame = (JFrame) w;

				if (title.trim().equalsIgnoreCase(frame.getTitle().trim())) {
					currentContainer = frame;
					currentContainer.setFocusTraversalKeysEnabled(true);
					currentContainer.setVisible(true);
					currentContainer.setEnabled(true);

					currentTitle = title;

					logger.debug("Navigate To (Frame): " + title);

					return;
				}
			}
		}		
		if (currentContainer == null){
			throw new BehaveException("Aplicação não carregada. Verifique as configurações do projeto");
		}
		throw new BehaveException("Tela não encontrada. \rContainer: \r" + currentContainer.toString() + "\r----------------------------------------------\rÁrvore de objetos: \r" + getHierarchy() + "\r----------------------------------------------");

	}

	public String getHierarchy() {
		ComponentPrinter printer = BasicComponentPrinter.printerWithCurrentAwtHierarchy();
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		PrintStream printStream = new PrintStream(out, true);
		printer.printComponents(printStream, currentContainer);
		printStream.flush();
		return new String(out.toByteArray());
	}

	@Override
	public String getCurrentUrl() {
		// Utilizado no screenshot
		return currentTitle;
	}

	@Override
	public String getTitle() {
		return currentTitle;
	}

	@Override
	public Element getElement(String currentPageName, String elementName) {
		if ((currentPageName == null) || (currentPageName.equals("")))
			throw new RuntimeException("Não existe tela selecionada.");

		ElementMap map = ReflectionUtil.getElementMap(currentPageName, elementName);
		ElementIndex index = DesktopReflectionUtil.getElementIndex(currentPageName, elementName);

		Class<?> clazz = ReflectionUtil.getElementType(currentPageName, elementName);

		DesktopElement element = null;
		// Comportamento padrão usa o InjectionManager para resolver quem implementa a interface
		if (clazz.isInterface())
			element = (DesktopElement) InjectionManager.getInstance().getInstanceDependecy(clazz);
		// Instancia a classe fornecida explicitamente como implementação da interface Element
		else if (Element.class.isAssignableFrom(clazz))
			try {
				element = (DesktopElement) clazz.newInstance();
			} catch (Exception e) {
				element = null;
			}
		else
			throw new RuntimeException("A class [" + clazz.getName() + "] no elemento [" + elementName + "] da tela [" + currentPageName + "] não é uma interface para 'Element'.");

		if (element == null)
			throw new RuntimeException("Não foi possível instanciar o elemento [" + elementName + "] da tela [" + currentPageName + "].");

		element.setElementMap(map);
		element.setElementIndex(index);

		return element;
	}

	@Override
	public String getPageSource() {
		return null;
	}

	@Override
	public void close() {

	}

	@Override
	public void quit() {
		mainFixture.cleanUp();
		mainContainer = null;
		currentContainer = null;
		mainFrame = null;
	}

	@Override
	public Object getDriver() {
		return robot;
	}

	@Override
	public Screen getScreen() {
		return (Screen) InjectionManager.getInstance().getInstanceDependecy(Screen.class);
	}

	public File saveScreenshotTo(String fileName) {
		File screenshotFile = new File(fileName);
		screenshotFile.getParentFile().mkdirs();
		ScreenshotTaker screenshotTaker = new ScreenshotTaker();
		screenshotTaker.saveDesktopAsPng(screenshotFile.getAbsolutePath());
		return screenshotFile;
	}

}
