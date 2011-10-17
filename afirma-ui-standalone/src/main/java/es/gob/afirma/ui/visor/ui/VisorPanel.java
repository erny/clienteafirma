/*
 * Este fichero forma parte del Cliente @firma.
 * El Cliente @firma es un aplicativo de libre distribucion cuyo codigo fuente puede ser consultado
 * y descargado desde www.ctt.map.es.
 * Copyright 2009,2010,2011 Gobierno de Espana
 * Este fichero se distribuye bajo licencia GPL version 3 segun las
 * condiciones que figuran en el fichero 'licence' que se acompana. Si se distribuyera este
 * fichero individualmente, deben incluirse aqui las condiciones expresadas alli.
 */

package es.gob.afirma.ui.visor.ui;

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JPanel;

import es.gob.afirma.core.misc.AOUtil;
import es.gob.afirma.signature.SignValidity;
import es.gob.afirma.signature.SignValidity.SIGN_DETAIL_TYPE;
import es.gob.afirma.signature.ValidateBinarySignature;
import es.gob.afirma.signature.ValidateXMLSignature;
import es.gob.afirma.signers.cades.AOCAdESSigner;
import es.gob.afirma.signers.cms.AOCMSSigner;
import es.gob.afirma.ui.utils.JAccessibilityDialogWizard;
import es.gob.afirma.ui.utils.Messages;
import es.gob.afirma.ui.utils.Utils;
import es.gob.afirma.ui.visor.DataAnalizerUtil;


/** Panel para la espera y detecci&oacute;n autom&aacute;tica de insercci&oacute;n de DNIe.
 * @author Tom&aacute;s Garc&iacute;a-Mer&aacute;s
 * @author Carlos Gamuci
 */
public final class VisorPanel extends JAccessibilityDialogWizard {

    /** Version ID */
    private static final long serialVersionUID = 8309157734617505338L;

    /** Construye un panel con la informaci&oacute;n extra&iacute;da de una firma. Si no se
     * indica la firma, esta se cargar&aacute; desde un fichero. Es obligatorio introducir
     * alguno de los dos par&aacute;metros. 
     * @param signFile Fichero de firma.
     * @param sign Firma.
     */
    public VisorPanel(final File signFile, final byte[] sign) {
        createUI(signFile, sign);
    }

    private void createUI(final File signFile, final byte[] sign) {
        this.setLayout(new GridBagLayout());

        openSign(signFile, sign);
    }

    /**
     * Analiza una firma indicada mediante un fichero o como un array de
     * bytes y muestra la informaci&oacute;n extra&iacute;da en un di&aacute;logo.
     * Si se indican ambos par&aacute;metros, se dar&aacute; prioridad al
     * byte array introducido.
     * @param signFile Fichero de firma.
     * @param signature Firma.
     */
    public void openSign(final File signFile, final byte[] signature) {

        if (signFile == null && signature == null) {
            Logger.getLogger("es.gob.afirma").warning("Se ha intentado abrir una firma nula");  //$NON-NLS-1$ //$NON-NLS-2$
            return;
        }
        
        byte[] sign = (signature != null) ?  signature.clone() : null;

        if (sign == null) {
            if (signFile != null) {
                try {
                    final FileInputStream fis = new FileInputStream(signFile);
                    sign = AOUtil.getDataFromInputStream(fis);
                    try { fis.close(); } catch (final Exception e) { /* Ignoramos los errores */ }
                }
                catch (final Exception e) {
                    Logger.getLogger("es.gob.afirma").warning("No se ha podido cargar el fichero de firma: " + e); //$NON-NLS-1$ //$NON-NLS-2$
                }
            }
        }

        SignValidity validity = new SignValidity(SIGN_DETAIL_TYPE.UNKNOWN, null);
        if (sign != null) {
            try {
                validity = validateSign(sign);
            } catch (final Exception e) {
                validity = new SignValidity(SIGN_DETAIL_TYPE.KO, null);
            }
        }

        final JPanel resultPanel = new SignResultPanel(validity);
        final JPanel dataPanel = new SignDataPanel(signFile, sign, null, null);

        
        final JButton bClose = new JButton(Messages.getString("VisorPanel.1")); //$NON-NLS-1$
        bClose.setToolTipText(Messages.getString("VisorPanel.2")); //$NON-NLS-1$
        bClose.getAccessibleContext().setAccessibleName(Messages.getString("VisorPanel.3"));  //$NON-NLS-1$
        
        Utils.remarcar(bClose);
        Utils.setContrastColor(bClose);
        Utils.setFontBold(bClose);
        
        bClose.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				VisorPanel.this.dispose();
			}
		});
        
        final JPanel bottonPanel = new JPanel(true);
        bottonPanel.setLayout(new FlowLayout(FlowLayout.TRAILING));
        bottonPanel.add(bClose);
        
        setLayout(new GridBagLayout());

        final GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1.0;
        c.insets = new Insets(11, 11, 11, 11);
        add(resultPanel, c);
        c.weighty = 1.0;
        c.gridy = 1;
        c.insets = new Insets(0, 11, 11, 11);
        add(dataPanel, c);
        c.weighty = 0.0;
        c.gridy = 2;
        c.insets = new Insets(0, 11, 11, 11);
        add(bottonPanel, c);

        repaint();
        
    }

    /**
     * Comprueba la validez de la firma.
     * @param sign Firma que se desea comprobar.
     * @return {@code true} si la firma es v&acute;lida, {@code false} en caso contrario.
     * @throws Exception Cuando los datos introducidos no se corresponden con una firma.
     */
    private SignValidity validateSign(final byte[] sign) throws Exception {
        if (DataAnalizerUtil.isPDF(sign)) {
            return new SignValidity(SIGN_DETAIL_TYPE.OK, null);
        } 
        else if (DataAnalizerUtil.isXML(sign)) {
            return ValidateXMLSignature.validate(sign);
        } 
        else if(new AOCMSSigner().isSign(sign) || new AOCAdESSigner().isSign(sign)) {
            return ValidateBinarySignature.validate(sign, null);
        }
        return new SignValidity(SIGN_DETAIL_TYPE.KO, null);
    }

    @Override
    public int getMinimumRelation() {
        return 0;
    }
}
