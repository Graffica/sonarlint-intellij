package org.sonarlint.intellij.config.project;

import com.google.common.collect.Maps;
import com.intellij.dvcs.repo.VcsRepositoryManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.AbstractTableCellEditor;
import org.sonarlint.intellij.config.ConfigurationPanel;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SonarLintVcsRootBindPanel {
    private final Project project;
    private JTable tblVcsMappings;
    private JButton btnRemoveVcs;
    private JButton btnAddVcs;
    private JPanel panel;
    private Map<String, String> originalMappings;
    private VcsRootMappingModel vcsRootMappingModel;

    public SonarLintVcsRootBindPanel(Project project) {
        this.project = project;
        btnAddVcs.addActionListener(e -> vcsRootMappingModel.addRow(new String[]{"", ""}));
        btnRemoveVcs.addActionListener(e -> vcsRootMappingModel.removeRow(tblVcsMappings.getSelectedRow()));
    }

    public JComponent getComponent() {
        return panel;
    }

    public Map<String, String> getVcsRootMappings() {
        int rowCount = vcsRootMappingModel.getRowCount();
        Map<String, String> mappings = Maps.newHashMapWithExpectedSize(rowCount);
        for (int row = 0; row < rowCount; row++) {
            mappings.put(vcsRootMappingModel.getValueAt(row, 0).toString(), vcsRootMappingModel.getValueAt(row, 1).toString());
        }
        return mappings;
    }

    public void setVcsRootMappings(Map<String, String> mappings) {
        mappings.forEach((r, k) -> vcsRootMappingModel.addRow(new String[]{r, k}));
        this.originalMappings = new HashMap<>(mappings);
    }

    private void createUIComponents() {
        tblVcsMappings = new JTable();
        vcsRootMappingModel = new VcsRootMappingModel();
        tblVcsMappings.setModel(vcsRootMappingModel);
        tblVcsMappings.getSelectionModel().addListSelectionListener(e -> btnRemoveVcs.setEnabled(e.getFirstIndex() > -1));
        tblVcsMappings.getColumnModel().getColumn(0).setCellEditor(new VcsRootCellEditor(collectVcsRoots()));

        panel = new JPanel();
        panel.setMinimumSize(new Dimension(100, 150));
        panel.setPreferredSize(new Dimension(100, 150));
    }

    private List<String> collectVcsRoots() {
        return Stream.of(ProjectLevelVcsManager.getInstance(project).getAllVcsRoots())
                .map(r -> r.getPath().getCanonicalPath()).collect(Collectors.toList());
    }

    private class VcsRootMappingModel extends DefaultTableModel {
        @Override
        public int getColumnCount() {
            return 2;
        }

        @Override
        public String getColumnName(int column) {
            switch (column) {
                case 0:
                    return "Vcs Root";
                case 1:
                    return "Project Key";
            }
            return super.getColumnName(column);
        }
    }

    private class VcsRootCellEditor extends AbstractTableCellEditor {
        private JComboBox<String> editor;

        public VcsRootCellEditor(List<String> roots) {
            editor = new JComboBox<>(new DefaultComboBoxModel<>(roots.toArray(new String[0])));
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            return editor;
        }

        @Override
        public Object getCellEditorValue() {
            return editor.getSelectedItem();
        }
    }
}
