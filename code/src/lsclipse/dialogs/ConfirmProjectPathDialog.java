package lsclipse.dialogs;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

public class ConfirmProjectPathDialog extends Dialog {
	private Combo cmbProj1;
	private Combo cmbProj2;
	
	private String proj1 = "";
	private String proj2 = "";
	
	private List<String> basePaths;
	private List<String> changePaths;

	public ConfirmProjectPathDialog(Shell parentShell) {
		super(parentShell);
		basePaths = new ArrayList<String>();
		changePaths = new ArrayList<String>();
	}
	
	public void addBasePath(String path) {
		basePaths.add(path);
	}
	
	public void addChangePath(String path) {
		changePaths.add(path);
	}
	
	public String getBasePath() {
		return proj1;
	}
	
	public String getChangePath() {
		return proj2;
	}
	
	public void okPressed() {
		proj1 = cmbProj1.getText();
		proj2 = cmbProj2.getText();

		super.okPressed();
	}

	protected Control createDialogArea(Composite parent) {
		this.getShell().setText("Confirm project paths");

		// overall layout
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		parent.setLayout(layout);

		// declare some layouts
		GridData ldtDefault = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
		ldtDefault.grabExcessHorizontalSpace = true;
		ldtDefault.grabExcessVerticalSpace = true;
		ldtDefault.horizontalAlignment = GridData.FILL;
		ldtDefault.verticalAlignment = GridData.FILL;
		ldtDefault.exclude = false;

		GridLayout panelLayout = new GridLayout();
		panelLayout.numColumns = 1;

		Composite leftPanel = new Composite(parent, 0);
		leftPanel.setLayoutData(new GridData(
				GridData.FILL_HORIZONTAL | GridData.FILL_VERTICAL | GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL));
		leftPanel.setLayout(panelLayout);

		Label base = new Label(leftPanel, 0);
		base.setText("Base Version:");

		// Project1 dropdown
		cmbProj1 = new Combo(leftPanel, SWT.DROP_DOWN);
		cmbProj1.setLayoutData(ldtDefault);

		Label changed = new Label(leftPanel, 0);
		changed.setText("Changed Version:");

		// Diff options
		cmbProj2 = new Combo(leftPanel, SWT.DROP_DOWN);
		cmbProj2.setLayoutData(ldtDefault);
		
		for (String path : basePaths) {
			cmbProj1.add(path);
		}
		
		for (String path : changePaths) {
			cmbProj2.add(path);
		}
		
		cmbProj1.select(0);
		cmbProj2.select(0);
		
		return parent;
	}
}
