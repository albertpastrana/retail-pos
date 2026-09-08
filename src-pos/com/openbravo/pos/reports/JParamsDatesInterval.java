//    Openbravo POS is a point of sales application designed for touch screens.
//    Copyright (C) 2007-2009 Openbravo, S.L.
//    http://www.openbravo.com/product/pos
//
//    This file is part of Openbravo POS.
//
//    Openbravo POS is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    Openbravo POS is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with Openbravo POS.  If not, see <http://www.gnu.org/licenses/>.

package com.openbravo.pos.reports;

import com.openbravo.pos.forms.AppView;
import java.awt.Component;
import java.util.Date;
import com.openbravo.beans.JCalendarDialog;
import com.openbravo.pos.forms.AppLocal;
import com.openbravo.data.loader.QBFCompareEnum;
import com.openbravo.format.Formats;
import com.openbravo.basic.BasicException;
import com.openbravo.data.loader.Datas;
import com.openbravo.data.loader.SerializerWrite;
import com.openbravo.data.loader.SerializerWriteBasic;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.Calendar;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class JParamsDatesInterval extends javax.swing.JPanel implements ReportEditorCreator {

    private static final int PRESET_TODAY = 0;
    private static final int PRESET_WEEK = 1;
    private static final int PRESET_MONTH = 2;
    private static final int PRESET_YEAR = 3;
    private static final int PRESET_CUSTOM = 4;

    private JComboBox jPreset;
    private JPanel customDates;

    /** Creates new form JParamsClosedPos */
    public JParamsDatesInterval() {
        initComponents();
        configureCompactLayout();
        jPreset.setSelectedIndex(PRESET_MONTH);
        applyPreset(PRESET_MONTH);
    }
    
    public void setStartDate(Date d) {
        jPreset.setSelectedIndex(PRESET_CUSTOM);
        jTxtStartDate.setText(Formats.TIMESTAMP.formatValue(d));
        jTxtEndDate.setText(null);
    }
    
    public void setEndDate(Date d) {
        jPreset.setSelectedIndex(PRESET_CUSTOM);
        jTxtEndDate.setText(Formats.TIMESTAMP.formatValue(d));
    }

    private void configureCompactLayout() {
        removeAll();
        setBorder(null);
        setPreferredSize(null);
        setLayout(new BorderLayout(4, 2));
        jTxtStartDate.setColumns(14);
        jTxtEndDate.setColumns(14);

        jPreset = new JComboBox(new String[] {
            AppLocal.getIntString("Report.Date.Today"),
            AppLocal.getIntString("Report.Date.ThisWeek"),
            AppLocal.getIntString("Report.Date.ThisMonth"),
            AppLocal.getIntString("Report.Date.ThisYear"),
            AppLocal.getIntString("Report.Date.Custom")
        });
        jPreset.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                applyPreset(jPreset.getSelectedIndex());
            }
        });

        JPanel presetPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        presetPanel.add(new JLabel(AppLocal.getIntString("Report.Date.Period")));
        presetPanel.add(jPreset);
        add(presetPanel, BorderLayout.NORTH);

        customDates = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        customDates.add(jLabel1);
        customDates.add(jTxtStartDate);
        customDates.add(btnDateStart);
        customDates.add(jLabel2);
        customDates.add(jTxtEndDate);
        customDates.add(btnDateEnd);
        add(customDates, BorderLayout.CENTER);
    }

    private void applyPreset(int preset) {
        boolean custom = preset == PRESET_CUSTOM;
        customDates.setVisible(custom);
        if (custom) {
            revalidate();
            return;
        }

        Calendar start = Calendar.getInstance();
        start.setTime(com.openbravo.beans.DateUtils.getToday());

        if (preset == PRESET_WEEK) {
            int daysSinceMonday = (start.get(Calendar.DAY_OF_WEEK) + 5) % 7;
            start.add(Calendar.DAY_OF_MONTH, -daysSinceMonday);
        } else if (preset == PRESET_MONTH) {
            start.set(Calendar.DAY_OF_MONTH, 1);
        } else if (preset == PRESET_YEAR) {
            start.set(Calendar.DAY_OF_YEAR, 1);
        }

        Calendar end = (Calendar) start.clone();
        if (preset == PRESET_TODAY) {
            end.add(Calendar.DAY_OF_MONTH, 1);
        } else if (preset == PRESET_WEEK) {
            end.add(Calendar.DAY_OF_MONTH, 7);
        } else if (preset == PRESET_MONTH) {
            end.add(Calendar.MONTH, 1);
        } else {
            end.add(Calendar.YEAR, 1);
        }

        jTxtStartDate.setText(Formats.TIMESTAMP.formatValue(start.getTime()));
        jTxtEndDate.setText(Formats.TIMESTAMP.formatValue(end.getTime()));
        revalidate();
    }

    public void init(AppView app) {
    }

    public void activate() throws BasicException {
    }
    
    public SerializerWrite getSerializerWrite() {
        return new SerializerWriteBasic(new Datas[] {Datas.OBJECT, Datas.TIMESTAMP, Datas.OBJECT, Datas.TIMESTAMP});
    }

    public Component getComponent() {
        return this;
    }
    
    public Object createValue() throws BasicException {
        Object startdate = Formats.TIMESTAMP.parseValue(jTxtStartDate.getText());
        Object enddate = Formats.TIMESTAMP.parseValue(jTxtEndDate.getText());   
        return new Object[] {
            startdate == null ? QBFCompareEnum.COMP_NONE : QBFCompareEnum.COMP_GREATEROREQUALS,
            startdate,
            enddate == null ? QBFCompareEnum.COMP_NONE : QBFCompareEnum.COMP_LESS,
            enddate
        };
    }    
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        jTxtStartDate = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        jTxtEndDate = new javax.swing.JTextField();
        btnDateStart = new javax.swing.JButton();
        btnDateEnd = new javax.swing.JButton();

        setBorder(javax.swing.BorderFactory.createTitledBorder(AppLocal.getIntString("label.bydates"))); // NOI18N
        setPreferredSize(new java.awt.Dimension(0, 100));
        setLayout(null);

        jLabel1.setText(AppLocal.getIntString("Label.StartDate")); // NOI18N
        add(jLabel1);
        jLabel1.setBounds(20, 20, 120, 15);
        add(jTxtStartDate);
        jTxtStartDate.setBounds(140, 20, 200, 19);

        jLabel2.setText(AppLocal.getIntString("Label.EndDate")); // NOI18N
        add(jLabel2);
        jLabel2.setBounds(20, 50, 120, 15);
        add(jTxtEndDate);
        jTxtEndDate.setBounds(140, 50, 200, 19);

        btnDateStart.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/openbravo/images/date.png"))); // NOI18N
        btnDateStart.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnDateStartActionPerformed(evt);
            }
        });
        add(btnDateStart);
        btnDateStart.setBounds(350, 20, 50, 26);

        btnDateEnd.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/openbravo/images/date.png"))); // NOI18N
        btnDateEnd.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnDateEndActionPerformed(evt);
            }
        });
        add(btnDateEnd);
        btnDateEnd.setBounds(350, 50, 50, 26);
    }// </editor-fold>//GEN-END:initComponents

    private void btnDateStartActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDateStartActionPerformed

        Date date;
        try {
            date = (Date) Formats.TIMESTAMP.parseValue(jTxtStartDate.getText());
        } catch (BasicException e) {
            date = null;
        }        
        date = JCalendarDialog.showCalendarTimeHours(this, date);
        if (date != null) {
            jTxtStartDate.setText(Formats.TIMESTAMP.formatValue(date));
        }             
    }//GEN-LAST:event_btnDateStartActionPerformed

    private void btnDateEndActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDateEndActionPerformed

        Date date;
        try {
            date = (Date) Formats.TIMESTAMP.parseValue(jTxtEndDate.getText());
        } catch (BasicException e) {
            date = null;
        }        
        date = JCalendarDialog.showCalendarTimeHours(this, date);
        if (date != null) {
            jTxtEndDate.setText(Formats.TIMESTAMP.formatValue(date));
        }          
    }//GEN-LAST:event_btnDateEndActionPerformed
    
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnDateEnd;
    private javax.swing.JButton btnDateStart;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JTextField jTxtEndDate;
    private javax.swing.JTextField jTxtStartDate;
    // End of variables declaration//GEN-END:variables
    
}

