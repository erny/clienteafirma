package es.gob.afirma.ui.core.jse;

import java.awt.Component;
import java.awt.FileDialog;
import java.awt.Frame;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.concurrent.CancellationException;

import javax.swing.JOptionPane;

import es.gob.afirma.core.AOCancelledOperationException;
import es.gob.afirma.core.misc.Platform;

/** Gestor de componentes de interfaz gr&aacute;fico (tanto para Applet como para
 * aplicaci&oacute;n de escritorio) de la aplicaci&oacute;n.
 * Esta clase usa AWT para los di&aacute;logos de apertura y guardado de ficheros.
 * @author Tom&aacute;s Garc&iacute;a-Mer&aacute;s */
public final class AWTUIManager extends JSEUIManager {

	/** {@inheritDoc} */
    @Override
	public File saveDataToFile(final byte[] data,
			                   final String dialogTitle,
			                   final File selectedFile,
			                   final String[] exts,
			                   final String description,
			                   final Object parent) throws IOException {
    	final FileDialog fd = new FileDialog(parent instanceof Frame ? (Frame) parent : null, dialogTitle, FileDialog.SAVE);
    	if (selectedFile != null) {
            fd.setDirectory(selectedFile.getAbsolutePath());
        }

    	if (exts != null) {
            fd.setFilenameFilter(new FilenameFilter() {
                @Override
                public boolean accept(final File dir, final String name) {
                    for (final String ext : exts) {
                        if (name.endsWith(ext)) {
                            return true;
                        }
                    }
                    return false;
                }
            });
        }

        fd.setVisible(true);

        final File file;

        if (fd.getFile() != null) {
            file = new File(fd.getDirectory() + fd.getFile());
        }
        else {
        	throw new AOCancelledOperationException();
        }

        try {
        	final FileOutputStream fos = new FileOutputStream(file);
            fos.write(data);
            fos.flush();
            fos.close();
        }
        catch (final Exception ex) {
            LOGGER.warning("No se pudo guardar la informacion en el fichero indicado: " + ex); //$NON-NLS-1$
            JOptionPane.showMessageDialog(
        		parent instanceof Component ? (Component) parent : null,
                JSEUIMessages.getString("JSEUIManager.88"), //$NON-NLS-1$
                JSEUIMessages.getString("JSEUIManager.89"), //$NON-NLS-1$
                JOptionPane.ERROR_MESSAGE
            );
        }

        return file;
    }

    /** {@inheritDoc} */
    @Override
	public String[] getLoadFileName(final String[] extensions, final String description, final boolean multiSelect, final Object parentComponent) {
    	if (multiSelect && isJava6()) {
    		return super.getLoadFileName(extensions, description, multiSelect, parentComponent);
    	}
    	return internalGetLoadFileName(null, null, extensions, multiSelect, parentComponent);
    }

    /** {@inheritDoc} */
    @Override
    public String[] getLoadFileName(final String dialogTitle, final String[] extensions, final String description, final boolean multiSelect, final Object parent) {
    	if (multiSelect && isJava6()) {
    		return super.getLoadFileName(dialogTitle, extensions, description, multiSelect, parent);
    	}
    	return internalGetLoadFileName(dialogTitle, null, extensions, multiSelect, parent);
    }

    @Override
	public File getLoadFile(final String dialogTitle, final String fileName, final String description, final Object parent) {
    	return new File(internalGetLoadFileName(dialogTitle, fileName, null, false, parent)[0]);
    }

	private static String[] internalGetLoadFileName(final String dialogTitle,
									 	    final String proposedFilename,
									 	    final String[] extensions,
									 	    final boolean multiSelect,
									 	    final Object parent) {
        final FileDialog fd = new FileDialog(parent instanceof Frame ? (Frame) parent : null, dialogTitle);
        fd.setMode(FileDialog.LOAD);

        // Habilitamos si corresponde el modo de seleccion multiple. Ya que solo esta disponible
        // en Java 7, lo hacemos por reflexion para evitar problemas de compilacion. Esto equivale
        // a la sentencia: fd.setMultipleMode(multiSelect);
        try {
        	final Method setMultipleModeMethod = FileDialog.class.getDeclaredMethod("setMultipleMode", Boolean.TYPE); //$NON-NLS-1$
        	setMultipleModeMethod.invoke(fd, Boolean.valueOf(multiSelect));
        } catch (final Exception e) {
        	LOGGER.warning("Error de reflexion al establecer el dialogo de carga con seleccion multiple, se realizara una seleccion simple: " + e); //$NON-NLS-1$
        }
        if (proposedFilename != null) {
        	fd.setDirectory(new File(proposedFilename).getAbsolutePath());
        }

        if (extensions != null && extensions.length > 0) {
        	// Los filtros de extension no funcionan en Windows
        	if (Platform.OS.WINDOWS.equals(Platform.getOS())) {
        		String ext = extensions[0];
        		if (!ext.startsWith("*.")) { //$NON-NLS-1$
        			if (ext.startsWith(".")) { //$NON-NLS-1$
        				ext = "*" + ext; //$NON-NLS-1$
        			}
        			else {
        				ext = "*." + ext; //$NON-NLS-1$
        			}
        		}
        		fd.setFile(ext);
        	}
        	else {
	            fd.setFilenameFilter(new FilenameFilter() {
	                @Override
	                public boolean accept(final File dir, final String name) {
	                    for (final String ext : extensions) {
	                        if (name.endsWith(ext)) {
	                            return true;
	                        }
	                    }
	                    return false;
	                }
	            });
        	}
        }
        fd.setVisible(true);
        if (fd.getFile() == null) {
            throw new CancellationException();
        }
        if (multiSelect) {

        	File[] files;
        	// Habilitamos si corresponde el modo de seleccion multiple. Ya que solo esta disponible
            // en Java 7, lo hacemos por reflexion para evitar problemas de compilacion. Esto equivale
            // a la sentencia: fd.getFiles();
        	try {
        		final Method getFilesMethod = FileDialog.class.getDeclaredMethod("getFiles", (Class<?>) null); //$NON-NLS-1$
        		files = (File[]) getFilesMethod.invoke(fd, (Object) null);
        	} catch (final Exception e) {
        		LOGGER.warning("Error de reflexion al recuperar la seleccion multiple del dialogo de carga, se devolvera un unico fichero: " + e); //$NON-NLS-1$
        		files = new File[] { new File(fd.getFile()) };
        	}
        	final String[] ret = new String[files.length];
        	for(int i=0;i<files.length;i++) {
        		ret[i] = files[i].getAbsolutePath();
        	}
        	return ret;
        }
        return new String[] { fd.getDirectory() + fd.getFile() };
    }

	private static boolean isJava6() {
		return System.getProperty("java.version").startsWith("1.6"); //$NON-NLS-1$ //$NON-NLS-2$
	}

}