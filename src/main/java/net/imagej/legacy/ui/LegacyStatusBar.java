/*
 * #%L
 * ImageJ software for multidimensional image processing and analysis.
 * %%
 * Copyright (C) 2009 - 2014 Board of Regents of the University of
 * Wisconsin-Madison, Broad Institute of MIT and Harvard, and Max Planck
 * Institute of Molecular Cell Biology and Genetics.
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

package net.imagej.legacy.ui;

import ij.IJ;

import java.awt.Panel;

import org.scijava.ui.StatusBar;
import org.scijava.widget.UIComponent;


/**
 * Adapter {@link StatusBar} implementation that delegates to {@link IJ}
 * methods.
 *
 * @author Mark Hiner
 */
public class LegacyStatusBar implements UIComponent<Panel>, StatusBar {

	/**
	 * As {@link IJ#showStatus(String)}
	 */
	@Override
	public void setStatus(String message) {
		IJ.showStatus(message);
	}

	/**
	 * As {@link IJ#showProgress(int, int)}
	 */
	@Override
	public void setProgress(int val, int max) {
		IJ.showProgress(val, max);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Panel getComponent() {
		return IJ.getInstance().getStatusBar();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Class<Panel> getComponentType() {
		return java.awt.Panel.class;
	}

}
