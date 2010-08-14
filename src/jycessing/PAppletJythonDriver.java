/*
 * Copyright 2010 Jonathan Feinberg
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package jycessing;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.regex.Pattern;

import org.python.core.CompileMode;
import org.python.core.CompilerFlags;
import org.python.core.Py;
import org.python.core.PyException;
import org.python.core.PyObject;
import org.python.core.PyStringMap;
import org.python.util.InteractiveConsole;

import processing.core.PApplet;
import processing.core.PConstants;

/**
 * 
 * @author Jonathan Feinberg &lt;jdf@pobox.com&gt;
 * 
 */
@SuppressWarnings("serial")
abstract public class PAppletJythonDriver extends PApplet {

    abstract protected void populateBuiltins();

    abstract protected void setFields();

    private static final PyObject NOMETH = new PyObject();
    protected final PyStringMap builtins;
    protected final InteractiveConsole interp;
    private final String pySketchPath;
    private final String programText;
    private final boolean isStaticMode;
    private PyObject setupMeth, drawMeth, mousePressedMeth, mouseClickedMeth,
            mouseReleasedMeth, mouseDraggedMeth, keyPressedMeth, keyReleasedMeth,
            keyTypedMeth;

    private static final Pattern ACTIVE_METHOD_DEF = Pattern.compile(
            "^def\\s+(setup|draw)\\s*\\(\\s*\\)\\s*:", Pattern.MULTILINE);

    private void executeSketch() {
        Py.setSystemState(interp.getSystemState());
        Py.exec(Py.compile_flags(programText, pySketchPath, CompileMode.exec,
                new CompilerFlags()), interp.getLocals(), null);
        Py.flushLine();
    }

    public PAppletJythonDriver(final InteractiveConsole interp, final String sketchPath,
            final String programText) {
        this.programText = programText;
        this.pySketchPath = sketchPath;
        this.isStaticMode = !ACTIVE_METHOD_DEF.matcher(programText).find();
        this.builtins = (PyStringMap) interp.getSystemState().getBuiltins();
        this.interp = interp;
        initializeStatics(builtins);
        populateBuiltins();
        setFields();
        builtins.__setitem__("this", Py.java2py(this));

        if (!isStaticMode) {
            executeSketch();
        }

        drawMeth = getMethod("draw");
        setupMeth = getMethod("setup");
        mousePressedMeth = getMethod("mousePressed");
        mouseClickedMeth = getMethod("mouseClicked");
        mouseReleasedMeth = getMethod("mouseReleased");
        mouseDraggedMeth = getMethod("mouseDragged");
        keyPressedMeth = getMethod("keyPressed");
        keyReleasedMeth = getMethod("keyReleased");
        keyTypedMeth = getMethod("keyTyped");
    }

    private PyObject getMethod(final String key) {
        final PyObject val = interp.get(key);
        return val == null ? NOMETH : val;
    }

    public static void initializeStatics(final PyStringMap builtins) {
        for (final Field f : PConstants.class.getDeclaredFields()) {
            final int mods = f.getModifiers();
            if (Modifier.isPublic(mods) || Modifier.isStatic(mods)) {
                try {
                    builtins.__setitem__(f.getName(), Py.java2py(f.get(null)));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void size(final int iwidth, final int iheight, final String irenderer,
            final String ipath) {
        super.size(iwidth, iheight, irenderer, ipath);
        setFields();
    }

    @Override
    public void setup() {
        setFields();
        if (isStaticMode) {
            executeSketch();
        } else if (setupMeth != NOMETH) {
            try {
                setupMeth.__call__();
            } catch (PyException e) {
                if (e.getCause() instanceof RendererChangeException) {
                    throw (RendererChangeException) e.getCause();
                } else {
                    throw e;
                }
            }
        }
    }

    @Override
    public void draw() {
        setFields();
        if (drawMeth == NOMETH) {
            super.draw();
        } else {
            drawMeth.__call__();
        }
    }

    @Override
    public void mouseClicked() {
        if (mouseClickedMeth == NOMETH) {
            super.mouseClicked();
        } else {
            mouseClickedMeth.__call__();
        }
    }

    @Override
    public void mousePressed() {
        if (mousePressedMeth == NOMETH) {
            super.mousePressed();
        } else {
            mousePressedMeth.__call__();
        }
    }

    @Override
    public void mouseReleased() {
        if (mouseReleasedMeth == NOMETH) {
            super.mouseReleased();
        } else {
            mouseReleasedMeth.__call__();
        }
    }

    @Override
    public void mouseDragged() {
        if (mouseDraggedMeth == NOMETH) {
            super.mouseDragged();
        } else {
            mouseDraggedMeth.__call__();
        }
    }

    @Override
    public void keyPressed() {
        if (keyPressedMeth == NOMETH) {
            super.keyPressed();
        } else {
            keyPressedMeth.__call__();
        }
    }

    @Override
    public void keyReleased() {
        if (keyReleasedMeth == NOMETH) {
            super.keyReleased();
        } else {
            keyReleasedMeth.__call__();
        }
    }

    @Override
    public void keyTyped() {
        if (keyTypedMeth == NOMETH) {
            super.keyTyped();
        } else {
            keyTypedMeth.__call__();
        }
    }
}
