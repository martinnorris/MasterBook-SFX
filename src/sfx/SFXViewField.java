package sfx;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public interface SFXViewField
{
	public SFXViewField createPanel(String scLabel, List<SFXViewListener> listListeners);
	public SFXViewField setValue(SFXBase field);
	public SFXViewField createContent(SFXBase field);
	public JComponent getPanel();
}

/* ==========================================================================
   Swing adaptations for components
   ========================================================================== */

interface SFXSwingControl
{
	public int getValue();
	public String getText();
}

class SFXSwingButton extends JButton implements SFXSwingControl
{
	private static final long serialVersionUID = -6995103706344921608L;
	private int m_iValue = 0;
	
	public SFXSwingButton createButton(int iValue)
	{
		m_iValue = iValue;
		return this;
	}

	@Override
	public int getValue() 
	{
		return m_iValue;
	}
}

class SFXSwingText extends JTextField implements SFXSwingControl
{
	private static final long serialVersionUID = -3208483646775964002L;
	public static final int _LINE = 50;
	public static final int _COLUMNS = 12;
	public static final int _COST = 6;

	@Override
	public int getValue() 
	{
		String scValue = this.getText();
		int iValue = Integer.parseInt(scValue);
		return iValue;
	}	
}

class SFXSwingCombo extends JComboBox<String> implements SFXSwingControl
{
	private static final long serialVersionUID = -6222288290185663340L;

	@Override
	public int getValue() 
	{
		return getSelectedIndex();
	}
	
	@Override
	public String getText()
	{
		return getSelectedItem().toString();
	}
	
	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append(getActionCommand());
		sb.append(' ');
		sb.append(Integer.toString(getSelectedIndex()));
		return sb.toString();
	}
}

class SFXSwingCheck extends JCheckBox implements SFXSwingControl
{
	private static final long serialVersionUID = -7870587884001243401L;

	@Override
	public int getValue() 
	{
		return isSelected()?1:0;
	}
}

class SFXSwingSlider extends JSlider implements SFXSwingControl
{
	private static final long serialVersionUID = 5907093220255910948L;
	
	private static final int _MINIMUM = 0;
	private static final int _MAXIMUM = 1000;
	
	private Hashtable<Integer, JLabel> m_tableNames = null;
	private float m_fMinimum = -1.0f;
	private float m_fMaximum = 1.0f;
	
	public SFXSwingSlider setLower(float fMinimum, String scMinimum)
	{
		m_fMinimum = fMinimum;
		
		setMinimum(_MINIMUM);
		setMaximum(_MAXIMUM);
		setMajorTickSpacing(_MAXIMUM);
		
		if (null==m_tableNames) m_tableNames = new Hashtable<Integer, JLabel>();	
		m_tableNames.put(new Integer(0), new JLabel(scMinimum));
		setLabelTable(m_tableNames);
		
		return this;
	}

	public SFXSwingSlider setUpper(float fMaximum, String scMaximum)
	{
		m_fMaximum = fMaximum;
		
		setMinimum(_MINIMUM);
		setMaximum(_MAXIMUM);
		setMajorTickSpacing(_MAXIMUM);
		
		if (null==m_tableNames) m_tableNames = new Hashtable<Integer, JLabel>();
		m_tableNames.put(new Integer(1000), new JLabel(scMaximum));
		setLabelTable(m_tableNames);
		
		return this;
	}
	
	@Override
	public String getText()
	{
		float fValue = getFraction();
		return Float.toString(fValue);
	}
	
	public float getFraction() 
	{
		int iSlider = super.getValue();
		
		float fRange = m_fMaximum - m_fMinimum;
		float fValue = (iSlider * fRange / _MAXIMUM) + m_fMinimum;
		
		return fValue;
	}
	
	public SFXSwingSlider setFraction(float fValue)
	{
		// Set the slider to the proportion of the real _MIN to _MAX range corresponding to limit set range
		float fRangeFrom = m_fMaximum - m_fMinimum;
		float fRangeTo = _MAXIMUM - _MINIMUM;
		int iValue = (int)((fValue - m_fMinimum) * fRangeTo / fRangeFrom) + _MINIMUM;
		setValue(iValue);		
		return this;
	}
}

class SFXSwingRadio extends JRadioButton implements SFXSwingControl
{
	private static final long serialVersionUID = -5447542731809998196L;
	
	private int m_iValue = 0;
	
	public SFXSwingRadio createButton(int iValue)
	{
		m_iValue = iValue;
		return this;
	}

	@Override
	public int getValue() 
	{
		return m_iValue;
	}
}

/* ==========================================================================
   Composite panels for the different fields
   ========================================================================== */

class SFXSwingBase extends JPanel implements SFXViewField, ActionListener
{
	private static final long serialVersionUID = 4203649691016574920L;
	
	protected SFXBase m_fieldChange = null;
	protected List<SFXViewListener> m_listListeners = null;
	
	// To get the panel to layout minimally set the maximum size as the preferred
	@Override
	public Dimension getMaximumSize()
	{
		Dimension dimMaximum = super.getMaximumSize();
		Dimension dimPreferred = getPreferredSize();
		Dimension dimReturn = new Dimension(dimMaximum.width, dimPreferred.height);
		return dimReturn;
	}
	
	@Override
	public SFXViewField createPanel(String scLabel, List<SFXViewListener> listListeners)
	{
		m_listListeners = listListeners;
		
		setLayout(new FlowLayout());
		
		JLabel label = new JLabel(scLabel);
		label.setHorizontalAlignment(SwingConstants.RIGHT);
		add(label);
						
		return populatePanel(scLabel, listListeners);
	}
	
	protected SFXViewField populatePanel(String scLabel, List<SFXViewListener> listListeners)
	{
		return this;
	}
	
	@Override
	public SFXViewField setValue(SFXBase field)
	{
		m_fieldChange = field;
		return this;
	}
	
	@Override
	public JComponent getPanel()
	{
		return this;
	}

	@Override
	public SFXViewField createContent(SFXBase field) 
	{
		// Default does not duplicate anything
		return this;
	}
	
	@Override
	public void actionPerformed(ActionEvent eventAction) 
	{
		SFXSwingControl control = (SFXSwingControl)eventAction.getSource();
		String scCommand = eventAction.getActionCommand();
		actionControl(control, scCommand);
		return;
	}
	
	protected SFXViewField actionControl(SFXSwingControl control, String scCommand)
	{
		// Button for +/-
		if (control instanceof SFXSwingButton) return modifyValue(control);
		// Value of textbox
		if (control instanceof SFXSwingText) return modifyText(control, control.getText());
		// Value of units combo box
		if (control instanceof SFXSwingCombo) return modifyMultiplier(control);
		// Check for 'follows'
		if (control instanceof SFXSwingCheck) return modifyDefault(control);
		// Radio buttons for DN/FV - picks up the DN/FV for the command
		if (control instanceof SFXSwingRadio) return modifyDNFV(control, control.getText());

		return this;
	}
	
	protected SFXViewField modifyValue(SFXSwingControl control)
	{
		for (SFXViewListener listener : m_listListeners)
			listener.modifyValue(control.getValue(), m_fieldChange);
		return this;
	}
	
	protected SFXViewField modifyText(SFXSwingControl control, String scCommand)
	{
		for (SFXViewListener listener : m_listListeners)
			listener.modifyValue(scCommand, m_fieldChange);
		return this;
	}	
	
	protected SFXViewField modifyMultiplier(SFXSwingControl control)
	{
		for (SFXViewListener listener : m_listListeners)
			listener.modifyMultiplier(control.getValue(), m_fieldChange);
		return this;
	}	
	
	protected SFXViewField modifyDefault(SFXSwingControl control)
	{
		String scCommand = 1==control.getValue()?"ENABLE":"DISABLE";
		for (SFXViewListener listener : m_listListeners)
			listener.modifyValue(scCommand, m_fieldChange);
		return this;
	}	
	
	protected SFXViewField modifyDNFV(SFXSwingControl control, String scCommand)
	{
		for (SFXViewListener listener : m_listListeners)
			listener.modifyValue(scCommand, m_fieldChange);
		return this;
	}	
}

class SFXSwingParent extends SFXSwingBase
{
	private static final long serialVersionUID = -1156182409719978897L;

	private String m_scLabel = null; 
	private Class<? extends SFXViewField> m_classChild = null;
	
	@Override
	public SFXViewField createPanel(String scLabel, List<SFXViewListener> listListeners)
	{
		// Use vertical layout
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		// With a simple border
		setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
		
		m_scLabel = scLabel;
		m_listListeners = listListeners;
		
		return this;
	}
	
	@Override
	public SFXViewField createContent(SFXBase field) 
	{
		return createInstance(field, m_classChild, m_scLabel);
	}
	
	protected SFXViewField createInstance(SFXBase field, Class<? extends SFXViewField> classChild, String scLabel) 
	{
		SFXViewField swingField = null;
		
		try 
		{
			swingField = (SFXViewField) classChild.newInstance();
			swingField.createPanel(scLabel, m_listListeners);
			swingField.setValue(field);
		} 
		catch (InstantiationException x) 
		{
			x.printStackTrace();
		} 
		catch (IllegalAccessException x) 
		{
			x.printStackTrace();
		}
		
		return swingField;
	}
	
	public SFXViewField setChild(Class<? extends SFXViewField> classChild)
	{
		m_classChild = classChild;
		return this;
	}
}

class SFXSwingSub extends SFXSwingBase
{
	private static final long serialVersionUID = -4981851979584602998L;
	
	protected SFXSwingText m_textValue = null;
	
	@Override
	protected SFXViewField populatePanel(String scLabel, List<SFXViewListener> listListeners) 
	{
		m_textValue = new SFXSwingText();
		m_textValue.setColumns(SFXSwingText._COST);
		m_textValue.setEditable(false);
		m_textValue.setForeground(Color.RED);
		add(m_textValue);
		
		return this;
	}	
	
	@Override
	public SFXViewField setValue(SFXBase field)
	{
		m_textValue.setText(field.getValue());
		return super.setValue(field);
	}
}

class SFXSwingScalar extends SFXSwingSub implements ActionListener
{
	private static final long serialVersionUID = 4217075003204072627L;

	protected SFXSwingText m_textDN = null;
	
	@Override
	protected SFXViewField populatePanel(String scLabel, List<SFXViewListener> listListeners) 
	{
		SFXSwingButton buttonUp = new SFXSwingButton();
		buttonUp.createButton(1).setText("+");
		buttonUp.addActionListener(this);
		add(buttonUp);
		
		m_textValue = new SFXSwingText();
		m_textValue.setColumns(SFXSwingText._COLUMNS);
		m_textValue.addActionListener(this);
		add(m_textValue);
		
		m_textDN = new SFXSwingText();
		m_textDN.setColumns(SFXSwingText._COST);
		m_textDN.setEditable(false);
		m_textDN.setForeground(Color.RED);
		add(m_textDN);
		
		SFXSwingButton buttonDown = new SFXSwingButton();
		buttonDown.createButton(-1).setText("-");
		buttonDown.addActionListener(this);
		add(buttonDown);
		
		return this;
	}
	
	@Override
	public SFXViewField setValue(SFXBase field)
	{
		// m_textValue set by parent
		
		int iCost = field.getCost();
		m_textDN.setText(Integer.toString(iCost));
		
		// SFXSwingSub sets the m_textValue to the value
		return super.setValue(field);
	}	
}

class SFXSwingValue extends SFXSwingScalar
{
	private static final long serialVersionUID = 8221028663744066657L;
	
	protected SFXSwingCombo m_comboUnits = null;
	
	@Override
	protected SFXViewField populatePanel(String scLabel, List<SFXViewListener> listListeners) 
	{
		super.populatePanel(scLabel, listListeners);
		
		m_comboUnits = new SFXSwingCombo();
		add(m_comboUnits);
		
		return this;
	}
	
	@Override
	public SFXViewField setValue(SFXBase field)
	{
		// Other fields set by parent
		
		SFXValue value = (SFXValue)field;
		
		int iCount = m_comboUnits.getItemCount();
		
		if (0==iCount)
		{
			String[] ascContents = value.getMultiplierContent();
			for (String scContent : ascContents)
				m_comboUnits.addItem(scContent);
		}
		
		int iMultiplierIndex = value.getMultiplierIndex();
		int iSelectedIndex = m_comboUnits.getSelectedIndex();
		
		if (iMultiplierIndex!=iSelectedIndex) m_comboUnits.setSelectedIndex(iMultiplierIndex);
		
		// Only listen to combo selection _after_ setting initial values because otherwise triggers value change before parents have their fields set which is a problem when loading existing data
		if (0==iCount) m_comboUnits.addActionListener(this);
		
		return super.setValue(field);
	}
}

class SFXSwingValueDefault extends SFXSwingValue
{
	private static final long serialVersionUID = -4597417358028124875L;	
	private SFXSwingCheck m_checkDefault = null;
	
	@Override
	protected SFXViewField populatePanel(String scLabel, List<SFXViewListener> listListeners) 
	{
		super.populatePanel(scLabel, listListeners);
		
		JLabel labelDefault = new JLabel("Necroscope");
		add(labelDefault);
		
		m_checkDefault = new SFXSwingCheck();
		m_checkDefault.addActionListener(this);
		add(m_checkDefault);
		
		return this;
	}
	
	@Override
	public SFXViewField setValue(SFXBase field)
	{
		SFXValueFollows follows = (SFXValueFollows)field;
		
		boolean zDefault = follows.getDefault();
		m_checkDefault.setSelected(zDefault);
		
		// Sets the other parts of the panel
		return super.setValue(field);
	}
}

class SFXSwingEnable extends SFXSwingScalar
{
	private static final long serialVersionUID = 4453109981509682299L;

	protected SFXSwingCheck m_checkEnable = null;
	
	@Override
	protected SFXViewField populatePanel(String scLabel, List<SFXViewListener> listListeners) 
	{
		// Text field needs to be created even though not used since it backs the field value
		m_textValue = new SFXSwingText();
		
		m_checkEnable = new SFXSwingCheck();
		m_checkEnable.addActionListener(this);
		add(m_checkEnable);
		
		m_textDN = new SFXSwingText();
		m_textDN.setColumns(SFXSwingText._COST);
		m_textDN.setEditable(false);
		m_textDN.setForeground(Color.RED);
		add(m_textDN);
		
		return this;
	}
	
	@Override
	public SFXViewField setValue(SFXBase field)
	{
		int iCost = field.getCost();
		m_checkEnable.setSelected(iCost!=0);
		return super.setValue(field);
	}
	
	@Override
	protected SFXViewField modifyDefault(SFXSwingControl control)
	{
		SFXSwingCheck check = (SFXSwingCheck)control;
		// Check box for field sets the value to 0 or 1
		String scState = check.isSelected()?"1":"0";
		return modifyText(control, scState);
	}
}

class SFXSwingParentArea extends SFXSwingParent
{
	private static final long serialVersionUID = -4114183866193507653L;

	@SuppressWarnings("rawtypes")
	private static Class[] sm_aclassSwingArea = 
	{
		SFXSwingScalar.class,
		SFXSwingScalar.class,
		SFXSwingChoice.class
	};
	
	private static String[] sm_ascLabels = 
	{
		"Area meters²",
		"Space meters³",
		"Shaped effect"
	};
	
	@SuppressWarnings("unchecked")
	@Override
	public SFXViewField createContent(SFXBase field) 
	{
		int iCount = getComponentCount();
		return createInstance(field, sm_aclassSwingArea[iCount], sm_ascLabels[iCount]);
	}
}

class SFXSwingParentDuration extends SFXSwingParent
{
	private static final long serialVersionUID = -1770796760481653068L;

	@SuppressWarnings("rawtypes")
	private static Class[] sm_aclassSwingDuration = 
	{
		SFXSwingChoice.class,
		SFXSwingValue.class
	};
	
	private static String[] sm_ascLabels = 
	{
		"Switch",
		"Extend"
	};
	
	@SuppressWarnings("unchecked")
	@Override
	public SFXViewField createContent(SFXBase field) 
	{
		int iCount = getComponentCount();
		return createInstance(field, sm_aclassSwingDuration[iCount], sm_ascLabels[iCount]);
	}	
}

class SFXSwingParentApportation extends SFXSwingParent
{
	private static final long serialVersionUID = -7383269577564819070L;

	@SuppressWarnings("rawtypes")
	private static Class[] sm_aclassSwingApportation = 
	{
		SFXSwingScalar.class,
		SFXSwingApportationMove.class
	};
	
	private static String[] sm_ascLabels = 
	{
		"ToHit +",
		"Move"
	};
	
	@SuppressWarnings("unchecked")
	@Override
	public SFXViewField createContent(SFXBase field) 
	{
		int iCount = getComponentCount();
		return createInstance(field, sm_aclassSwingApportation[iCount], sm_ascLabels[iCount]);
	}		
}

class SFXSwingApportationMove extends SFXSwingEnable
{
	private static final long serialVersionUID = 7354750762312333666L;

	private SFXSwingCombo m_comboMassUnits = null;
	private SFXSwingText m_textSpeed = null;
	private SFXSwingCombo m_comboSpeedUnits = null;

	@Override
	protected SFXViewField populatePanel(String scLabel, List<SFXViewListener> listListeners) 
	{
		super.populatePanel(scLabel, listListeners);
		
		// Add more fields "+ <value weight> - <weight units drop> <value speed> <speed units drop>"
		
		SFXSwingButton buttonUp = new SFXSwingButton();
		buttonUp.createButton(1).setText("+");
		buttonUp.addActionListener(this);
		add(buttonUp);
		
		m_textValue = new SFXSwingText();
		m_textValue.setColumns(SFXSwingText._COST);
		m_textValue.addActionListener(this);
		add(m_textValue);
		
		SFXSwingButton buttonDown = new SFXSwingButton();
		buttonDown.createButton(-1).setText("-");
		buttonDown.addActionListener(this);
		add(buttonDown);
		
		m_comboMassUnits = new SFXSwingCombo();
		add(m_comboMassUnits);
		
		m_textSpeed = new SFXSwingText();
		m_textSpeed.setColumns(SFXSwingText._COST);	
		add(m_textSpeed);
		
		m_comboSpeedUnits = new SFXSwingCombo();
		add(m_comboSpeedUnits);
		
		return this;
	}	
	
	@Override
	public SFXViewField setValue(SFXBase field)
	{
		super.setValue(field);
		
		SFXValueMove valueMove = (SFXValueMove) field;		
		m_checkEnable.setSelected(valueMove.getDefault());
		
		int iCountWeight = m_comboMassUnits.getItemCount();
		
		if (0==iCountWeight)
		{
			String[] ascContents = valueMove.getMultiplierContent();
			for (String scContent : ascContents)
				m_comboMassUnits.addItem(scContent);
			m_comboMassUnits.addActionListener(this);
		}
		
		int iCountSpeed = m_comboSpeedUnits.getItemCount();
		
		if (0==iCountSpeed)
		{
			String[] ascContents = valueMove.getMultiplierContentEx();
			for (String scContent : ascContents)
				m_comboSpeedUnits.addItem(scContent);
			m_comboSpeedUnits.addActionListener(this);
		}
		
		return updateCalculated(field);
	}
	
	public int getMoveOffset()
	{
		return m_comboSpeedUnits.getValue();
	}
	
	private SFXViewField updateCalculated(SFXBase field)
	{	
		SFXValueMove valueMove = (SFXValueMove) field;
		
		float fTry = valueMove.getMass();
		int iOffsetMass = m_comboMassUnits.getValue();
		int iDeltaEffectMassCost = valueMove.getDeltaEffectMassCost(fTry, iOffsetMass);
		
		int iOffsetMove = getMoveOffset();
		float fMove = valueMove.getMoveCost(iDeltaEffectMassCost, iOffsetMove);
		
		String scValue;
		
		if (0>fMove)
			scValue = "Weight cannot be moved";
		else
			scValue = Float.toString(fMove);	
		
		m_textSpeed.setText(scValue);
				
		return this;
	}
	
	@Override
	protected SFXViewField actionControl(SFXSwingControl control, String scCommand)
	{
		// Only update the calculated value nothing changes on the underlying field
		if (control instanceof SFXSwingCombo) return modifyCalculated(m_fieldChange);
		// check box {for enable parent} sets value of field to 0 or 1 so better to use text to set ENABLE or DISABLE
		if (control instanceof SFXSwingCheck) return modifyText(control, 1==control.getValue()?"ENABLE":"DISABLE");
		// everything else 
		return super.actionControl(control, scCommand);
	}
	
	private SFXViewField modifyCalculated(SFXBase field)
	{
		updateCalculated(field);
		// Updating the calculated values in the view does not change the underlying model
		// But there are listeners {e.g. TextPane that need to change}
		field.changedField();
		return this;
	}	
}

class SFXSwingCharges extends SFXSwingScalar
{
	private static final long serialVersionUID = -3135701004149666620L;

	private SFXSwingCheck m_checkWard = null;
	
	@Override
	protected SFXViewField populatePanel(String scLabel, List<SFXViewListener> listListeners) 
	{
		super.populatePanel(scLabel, listListeners);
		
		JLabel labelWard = new JLabel("Ward");
		add(labelWard);
		
		m_checkWard = new SFXSwingCheck();
		m_checkWard.addActionListener(this);
		add(m_checkWard);
		
		return this;
	}
	
	@Override
	public SFXViewField setValue(SFXBase field)
	{
		SFXFieldCharges scalarCharges = (SFXFieldCharges) field;
		int iBonus = scalarCharges.getBonus();
		m_checkWard.setSelected(0==iBonus?false:true);
		return super.setValue(field);
	}
	
	@Override
	protected SFXViewField actionControl(SFXSwingControl control, String scCommand)
	{
		if (control instanceof SFXSwingCheck)
		{
			// The 'ward' checkbox sends a value by string
			String scState = String.format("%s %s", m_textValue.getText(), m_checkWard.isSelected()?"true":"false");
			return modifyText(control, scState);
		}
		return super.actionControl(control, scCommand);
	}
	
	@Override
	protected SFXViewField modifyValue(SFXSwingControl control)
	{
		for (SFXViewListener listener : m_listListeners)
			listener.modifyValue(control.getValue() * 5, m_fieldChange);
		return this;
	}
}

class SFXSwingProportion extends SFXSwingScalar implements ChangeListener
{
	private static final long serialVersionUID = -2552361922280671709L;
	
	private static final float _MINIMUM = 1.0f/3.0f;
	private static final float _MAXIMUM = 2.0f/3.0f;
	
	protected SFXSwingSlider m_sliderFraction = null;

	@Override
	protected SFXViewField populatePanel(String scLabel, List<SFXViewListener> listListeners) 
	{
		// Need even if not used
		m_textValue = new SFXSwingText();
		m_textDN = new SFXSwingText();
		
		// Slider is 0 to 1000 which corresponds to 1/3 to 2/3
		m_sliderFraction = new SFXSwingSlider();
		m_sliderFraction.setLower(_MINIMUM, "DN").setUpper(_MAXIMUM, "FV").setPaintLabels(true);
		m_sliderFraction.addChangeListener(this);
		add(m_sliderFraction);
		
		JLabel label5050 = new JLabel("Necroscope");
		add(label5050);
		
		SFXSwingCheck checkDefault = new SFXSwingCheck();
		checkDefault.addActionListener(this);
		add(checkDefault);
		
		return this;
	}
	
	@Override
	public SFXViewField setValue(SFXBase field)
	{
		SFXFraction fraction = (SFXFraction)field;
		int iNumerator = fraction.getCost(100);
		int iDenominator = 100 - fraction.getCost(100);
		float fValue = iNumerator / (float)(iNumerator + iDenominator);
		m_sliderFraction.setFraction(fValue);
		
		// SFXSwingSub sets the m_textValue to the value
		// SFXSwingScalar sets m_textDN to cost
		return super.setValue(field);
	}
	
	@Override
	protected SFXViewField modifyDefault(SFXSwingControl control)
	{
		int iValue = control.getValue();
		
		if (0<iValue)
		{
			// When check set then use default value
			return modifyText(control, "DEFAULT");			
		}
		
		// Set the slider to the current (default) value
		SFXFraction fraction = (SFXFraction)m_fieldChange;
		
		int iNumerator = fraction.getCost(100);
		int iDenominator = 100 - fraction.getCost(100);
		
		float fValue = iNumerator / (float)(iNumerator + iDenominator);
		m_sliderFraction.setFraction(fValue);
		
		return modifyText(control, "FRACTION");
	}	
	
	@Override
	public void stateChanged(ChangeEvent eventChange) 
	{
		SFXSwingSlider control = (SFXSwingSlider)eventChange.getSource();
		// Ignore state change till calm
		if (control.getValueIsAdjusting()) return;
		float fValue = control.getFraction();
		String scValue = Float.toString(fValue);
		modifyText(control, scValue);
		return;
	}	
}

class SFXSwingChoice extends SFXSwingValue
{
	private static final long serialVersionUID = 4846525697067278262L;

	@Override
	protected SFXViewField populatePanel(String scLabel, List<SFXViewListener> listListeners) 
	{
		// Need to set even if not used
		m_textValue = new SFXSwingText();
		
		m_comboUnits = new SFXSwingCombo();
		add(m_comboUnits);
		
		m_textDN = new SFXSwingText();
		m_textDN.setColumns(SFXSwingText._COST);
		m_textDN.setEditable(false);
		m_textDN.setForeground(Color.RED);
		add(m_textDN);
		
		return this;		
	}	
	
	@Override
	public SFXViewField setValue(SFXBase field)
	{
		SFXValue value = (SFXValue)field;
		
		int iCount = m_comboUnits.getItemCount();
		
		if (0==iCount)		
		{
			String[] ascContents = value.getMultiplierContent();
			
			for (int iIndex = 0; ascContents.length>iIndex; ++iIndex)
				m_comboUnits.addItem(ascContents[iIndex]);
		}
		
		// Have to go up the inheritance tree to SFXSwingScalar to set the cost correctly
		int iCost = field.getCost();
		m_textDN.setText(Integer.toString(iCost));
		
		// Set the combo box index		
		int iMultiplierIndex = value.getMultiplierIndex();
		int iSelectedIndex = m_comboUnits.getSelectedIndex();	
		if (iMultiplierIndex!=iSelectedIndex) m_comboUnits.setSelectedIndex(iMultiplierIndex);
		
		// Only listen to combo selection _after_ setting initial values because otherwise triggers value change before parents have their fields set which is a problem when loading existing data
		if (0==iCount) m_comboUnits.addActionListener(this);
		
		return super.setValue(field);
	}	
}

class SFXSwingConcentration extends SFXSwingValue
{
	private static final long serialVersionUID = -7903344250107008710L;
	
	protected SFXSwingText m_textFV = null;
	protected JLabel m_labelComment = null;

	@Override
	protected SFXViewField populatePanel(String scLabel, List<SFXViewListener> listListeners) 
	{
		SFXSwingButton buttonUp = new SFXSwingButton();
		buttonUp.createButton(1).setText("+");
		buttonUp.addActionListener(this);
		add(buttonUp);
		
		m_textValue = new SFXSwingText();
		m_textValue.setColumns(SFXSwingText._COLUMNS);
		m_textValue.addActionListener(this);
		add(m_textValue);
		
		SFXSwingButton buttonDown = new SFXSwingButton();
		buttonDown.createButton(-1).setText("-");
		buttonDown.addActionListener(this);
		add(buttonDown);
		
		m_comboUnits = new SFXSwingCombo();
		add(m_comboUnits);
		
		JLabel labelApply = new JLabel("Apply to");
		add(labelApply);
		
		SFXSwingRadio buttonDN = new SFXSwingRadio();
		buttonDN.createButton(1).setText("DN");
		buttonDN.addActionListener(this);
		add(buttonDN);
		
		m_textDN = new SFXSwingText();
		m_textDN.setColumns(SFXSwingText._COST);
		m_textDN.setEditable(false);
		m_textDN.setForeground(Color.RED);
		add(m_textDN);
		
		m_textFV = new SFXSwingText();
		m_textFV.setColumns(SFXSwingText._COST);
		m_textFV.setEditable(false);
		m_textFV.setForeground(Color.BLUE);
		add(m_textFV);
		
		SFXSwingRadio buttonFV = new SFXSwingRadio();
		buttonFV.createButton(-1).setText("FV");
		buttonFV.addActionListener(this);
		add(buttonFV);
		
		ButtonGroup group = new ButtonGroup();
		group.add(buttonDN);
		group.add(buttonFV);
		
		m_labelComment = new JLabel("Will @ DN ???");
		add(m_labelComment);
		
		return this;
	}
	
	@Override
	public SFXViewField setValue(SFXBase field)
	{
		SFXValueModifier valueModifier = (SFXValueModifier)field;
		
		int iReduceFV = valueModifier.getReduction();
		m_textFV.setText(Integer.toString(iReduceFV));
		
		m_labelComment.setText(valueModifier.getComment());
		
		return super.setValue(field);
	}	
}

class SFXSwingDouble extends SFXSwingBase
{
	private static final long serialVersionUID = -7903344250107008710L;
	
	protected SFXSwingText m_textDN = null;
	protected SFXSwingText m_textFV = null;

	@Override
	protected SFXViewField populatePanel(String scLabel, List<SFXViewListener> listListeners) 
	{
		m_textDN = new SFXSwingText();
		m_textDN.setColumns(SFXSwingText._COST);
		m_textDN.setEditable(false);
		m_textDN.setForeground(Color.RED);
		add(m_textDN);
		
		m_textFV = new SFXSwingText();
		m_textFV.setColumns(SFXSwingText._COST);
		m_textFV.setEditable(false);
		m_textFV.setForeground(Color.BLUE);
		add(m_textFV);
		
		return this;
	}
	
	@Override
	public SFXViewField setValue(SFXBase field)
	{
		SFXDouble valueModifier = (SFXDouble)field;
		
		int iValueDN = valueModifier.getValueDN();
		m_textDN.setText(Integer.toString(iValueDN));

		int iValueFV = valueModifier.getValueFV();
		m_textFV.setText(Integer.toString(iValueFV));
		
		return super.setValue(field);
	}
}

class SFXSwingOthers extends SFXSwingDouble implements ActionListener
{
	private static final long serialVersionUID = -3360427612581410190L;
	
	@Override
	protected SFXViewField populatePanel(String scLabel, List<SFXViewListener> listListeners) 
	{
		m_textDN = new SFXSwingText();
		m_textDN.setColumns(SFXSwingText._COST);
		m_textDN.setForeground(Color.RED);
		m_textDN.setActionCommand("DN");
		m_textDN.addActionListener(this);
		add(m_textDN);
		
		m_textFV = new SFXSwingText();
		m_textFV.setColumns(SFXSwingText._COST);
		m_textFV.setForeground(Color.BLUE);
		m_textFV.setActionCommand("FV");
		m_textFV.addActionListener(this);
		add(m_textFV);
		
		return this;
	}

	@Override
	protected SFXViewField actionControl(SFXSwingControl control, String scCommand)
	{
		if (control instanceof SFXSwingText)
		{
			String scCommandValue = String.format("%s %s", scCommand, control.getText());
			return modifyText(control, scCommandValue);
		}
		
		return super.actionControl(control, scCommand);
	}
}

class SFXSwingCommunity extends SFXSwingConcentration
{
	private static final long serialVersionUID = 2845023364381383354L;

	private SFXSwingCombo m_comboSize = null;
	private SFXSwingButton m_buttonRemove = null;
	
	@Override
	public SFXViewField createContent(SFXBase field)
	{
		SFXSwingCommunity community = new SFXSwingCommunity();
		community.createPanel("Community", m_listListeners);
		community.setValue(field);
		return community;
	}
	
	@Override
	protected SFXViewField populatePanel(String scLabel, List<SFXViewListener> listListeners) 
	{
		// Need m_textValue even if not used 
		m_textValue = new SFXSwingText();
		// Need comment even if not used
		m_labelComment = new JLabel();
		
		m_comboSize = new SFXSwingCombo();
		m_comboSize.setActionCommand("Size");
		add(m_comboSize);
				
		m_comboUnits = new SFXSwingCombo();
		m_comboUnits.setActionCommand("DN");
		add(m_comboUnits);
		
		JLabel labelApply = new JLabel("Apply to");
		add(labelApply);
		
		SFXSwingRadio buttonDN = new SFXSwingRadio();
		buttonDN.createButton(1).setText("DN");
		buttonDN.addActionListener(this);
		add(buttonDN);
		
		m_textDN = new SFXSwingText();
		m_textDN.setColumns(SFXSwingText._COST);
		m_textDN.setEditable(false);
		m_textDN.setForeground(Color.RED);
		add(m_textDN);
		
		m_textFV = new SFXSwingText();
		m_textFV.setColumns(SFXSwingText._COST);
		m_textFV.setEditable(false);
		m_textFV.setForeground(Color.BLUE);
		add(m_textFV);
		
		SFXSwingRadio buttonFV = new SFXSwingRadio();
		buttonFV.createButton(-1).setText("FV");
		buttonFV.addActionListener(this);
		add(buttonFV);
		
		ButtonGroup group = new ButtonGroup();
		group.add(buttonDN);
		group.add(buttonFV);
		
		SFXSwingButton buttonMore = new SFXSwingButton();
		buttonMore.createButton(1).setText("Add");
		buttonMore.addActionListener(this);
		add(buttonMore);
		
		m_buttonRemove = new SFXSwingButton();
		m_buttonRemove.createButton(1).setText("Remove");
		m_buttonRemove.addActionListener(this);
		add(m_buttonRemove);
		
		return this;
	}
	
	@Override
	public SFXViewField setValue(SFXBase field)
	{
		SFXFieldCommunity valueModifier = (SFXFieldCommunity)field;
		
		int iCountSize = m_comboSize.getItemCount();
		
		if (0==iCountSize)
		{
			String[] ascContents = valueModifier.getTableContent();
			for (String scContent : ascContents)
				m_comboSize.addItem(scContent);
		}
		
		int iCountUnits = m_comboUnits.getItemCount();
		
		if (0==iCountUnits)
		{
			String[] ascContents = valueModifier.getMultiplierContent();
			for (String scContent : ascContents)
				m_comboUnits.addItem(scContent);
		}
		
		int iReduceDN = valueModifier.getCost();
		m_textDN.setText(Integer.toString(iReduceDN));
		int iReduceFV = valueModifier.getReduction();
		m_textFV.setText(Integer.toString(iReduceFV));
		
		String scValue = field.getValue();
		int iValueIndex = Integer.parseInt(scValue);
		int iSizeIndex = m_comboSize.getSelectedIndex();
		if (iValueIndex!=iSizeIndex) m_comboSize.setSelectedIndex(iValueIndex);
		
		int iMultiplierIndex = valueModifier.getMultiplierIndex();		
		int iDNIndex = m_comboUnits.getSelectedIndex();
		if (iMultiplierIndex!=iDNIndex) m_comboUnits.setSelectedIndex(iMultiplierIndex);
		
		boolean zFirst = valueModifier.isInitial();
		m_buttonRemove.setEnabled(!zFirst);
		
		// Only listen to combo selection _after_ setting initial values because otherwise triggers value change before parents have their fields set which is a problem when loading existing data
		if (0==iCountSize) m_comboSize.addActionListener(this);
		if (0==iCountUnits) m_comboUnits.addActionListener(this);
		
		return super.setValue(field);
	}
	
	@Override
	protected SFXViewField actionControl(SFXSwingControl control, String scCommand)
	{
		if (control instanceof SFXSwingCombo)
		{
			if (scCommand.equals("Size")) return modifyValue(control);
			if (scCommand.equals("DN")) return modifyMultiplier(control);		
		}
		
		if (control instanceof SFXSwingButton)
		{
			SFXSwingButton button = (SFXSwingButton)control;
			if (scCommand.equals("Add")) return createField(button);
			if (scCommand.equals("Remove") && m_buttonRemove.isEnabled()) return removeField(button);
		}
		
		return super.actionControl(control, scCommand);
	}
	
	protected SFXSwingCommunity createField(SFXSwingButton button)
	{
		for (SFXViewListener listener : m_listListeners)
			listener.duplicateField(m_fieldChange);
		return this;
	}	
	
	protected SFXSwingCommunity removeField(SFXSwingButton button)
	{
		for (SFXViewListener listener : m_listListeners)
			listener.removeField(m_fieldChange);
		return this;
	}	
}

class SFXSwingComponent extends SFXSwingCommunity
{
	private static final long serialVersionUID = 3910005708152402615L;
	
	private Color m_colourDefault = null;

	@Override
	public SFXViewField createContent(SFXBase field)
	{
		SFXSwingComponent component = new SFXSwingComponent();
		component.createPanel("Component", m_listListeners);
		component.setValue(field);
		return component;
	}
	
	@Override
	protected SFXViewField populatePanel(String scLabel, List<SFXViewListener> listListeners) 
	{
		super.populatePanel(scLabel, listListeners);
		
		JLabel labelDestroy = new JLabel("Destroy");
		add(labelDestroy, 2);
		
		SFXSwingCheck checkDestroy = new SFXSwingCheck();
		checkDestroy.setActionCommand("Destroy");
		checkDestroy.addActionListener(this);
		add(checkDestroy, 3);
		
		m_colourDefault = m_textDN.getBackground();
		
		return this;
	}
	
	@Override
	public SFXViewField setValue(SFXBase field)
	{
		SFXCompositeComponents compositeComponent = (SFXCompositeComponents) field.getParent();
		boolean zLimited = compositeComponent.isLimited();
		
		if (zLimited)
		{
			m_textDN.setBackground(Color.YELLOW);
			m_textFV.setBackground(Color.YELLOW);
		}
		else
		{
			m_textDN.setBackground(m_colourDefault);
			m_textFV.setBackground(m_colourDefault);			
		}
		
		return super.setValue(field);
	}
	
	@Override
	protected SFXViewField actionControl(SFXSwingControl control, String scCommand)
	{
		if (control instanceof SFXSwingCheck)
		{
			SFXSwingCheck check = (SFXSwingCheck)control;
			if (scCommand.equals("Destroy") && check.isSelected()) return modifyText(control, "DESTROY");
			if (scCommand.equals("Destroy") && !check.isSelected()) return modifyText(control, "PRESERVE");
		}
		// Handles all the other controls
		return super.actionControl(control, scCommand);
	}	
}

class SFXSwingGesture extends SFXSwingCommunity
{
	private static final long serialVersionUID = -5433880564702130002L;

	@Override
	public SFXViewField populatePanel(String scLabel, List<SFXViewListener> listListeners)
	{
		super.populatePanel(scLabel, listListeners);

		m_labelComment = new JLabel("Will @ DN ???");
		add(m_labelComment);
		
		return this;
	}
	
	@Override
	public SFXViewField setValue(SFXBase field)
	{
		SFXValueModifier valueModifier = (SFXValueModifier)field;
		m_labelComment.setText(valueModifier.getComment());
		return super.setValue(field);
	}
	
	@Override
	public SFXViewField createContent(SFXBase field)
	{
		SFXSwingGesture gesture = new SFXSwingGesture();
		gesture.createPanel("Gesture", m_listListeners);
		gesture.setValue(field);
		return gesture;
	}
}

class SFXSwingIncantation extends SFXSwingConcentration
{
	private static final long serialVersionUID = 883150629205412601L;

	@Override
	public SFXViewField populatePanel(String scLabel, List<SFXViewListener> listListeners)
	{
		// Not used but needs to be created
		m_textValue = new SFXSwingText();
		
		m_comboUnits = new SFXSwingCombo();
		add(m_comboUnits);
		
		JLabel labelForeign = new JLabel("Foreign");
		add(labelForeign);
		
		SFXSwingCheck checkForeign = new SFXSwingCheck();
		checkForeign.setActionCommand("Foreign");
		checkForeign.addActionListener(this);
		add(checkForeign);
		
		JLabel labelLoud = new JLabel("Loud");
		add(labelLoud);
		
		SFXSwingCheck checkLoud = new SFXSwingCheck();
		checkLoud.setActionCommand("Loud");
		checkLoud.addActionListener(this);
		add(checkLoud);
		
		JLabel labelProfane = new JLabel("Profane");
		add(labelProfane);
		
		SFXSwingCheck checkProfane = new SFXSwingCheck();
		checkProfane.setActionCommand("Profane");
		checkProfane.addActionListener(this);
		add(checkProfane);
		
		JLabel labelApply = new JLabel("Apply to");
		add(labelApply);
		
		SFXSwingRadio buttonDN = new SFXSwingRadio();
		buttonDN.createButton(1).setText("DN");
		buttonDN.addActionListener(this);
		add(buttonDN);
		
		m_textDN = new SFXSwingText();
		m_textDN.setColumns(SFXSwingText._COST);
		m_textDN.setEditable(false);
		m_textDN.setForeground(Color.RED);
		add(m_textDN);
		
		m_textFV = new SFXSwingText();
		m_textFV.setColumns(SFXSwingText._COST);
		m_textFV.setEditable(false);
		m_textFV.setForeground(Color.BLUE);
		add(m_textFV);
		
		SFXSwingRadio buttonFV = new SFXSwingRadio();
		buttonFV.createButton(-1).setText("FV");
		buttonFV.addActionListener(this);
		add(buttonFV);
		
		ButtonGroup group = new ButtonGroup();
		group.add(buttonDN);
		group.add(buttonFV);
		
		m_labelComment = new JLabel("Will @ DN ???");
		add(m_labelComment);
		
		return this;
	}
	
	@Override
	public SFXViewField setValue(SFXBase field)
	{
		SFXFieldIncantation valueIncantation = (SFXFieldIncantation) field;
		
		int iBonus = valueIncantation.getBonus();
		
		setButton("Foreign", 0==(iBonus & SFXFieldIncantation._FOREIGN)?false:true); 
		setButton("Loud", 0==(iBonus & SFXFieldIncantation._LOUD)?false:true); 
		setButton("Profane", 0==(iBonus & SFXFieldIncantation._PROFANE)?false:true); 
		
		return super.setValue(field);
	}
	
	private SFXViewField setButton(String scName, boolean zState)
	{
		Component[] aComponents = getComponents();
		
		for (int iIndex = 0; iIndex<aComponents.length; ++iIndex)
		{
			Component componentChild = aComponents[iIndex];
			if (!(componentChild instanceof SFXSwingCheck)) continue;
			SFXSwingCheck check = (SFXSwingCheck) componentChild;
			String scCommand = check.getActionCommand();
			if (!scCommand.equals(scName)) continue;
			check.setSelected(zState);
			return this;
		}
		
		return this;
	}
	
	@Override
	protected SFXViewField actionControl(SFXSwingControl control, String scCommand)
	{
		if (control instanceof SFXSwingCheck)
		{
			boolean zState = ((SFXSwingCheck)control).isSelected();
			
			if (scCommand.equals("Foreign")) return modifyText(control, zState?"FOREIGN":"NATIVE");
			if (scCommand.equals("Loud")) return modifyText(control, zState?"LOUD":"SOFT");
			if (scCommand.equals("Profane")) return modifyText(control, zState?"PROFANE":"POLITE");
		}

		return super.actionControl(control, scCommand);
	}
}

class SFXSwingRelatedSkill extends SFXSwingConcentration
{
	private static final long serialVersionUID = -178105703624717481L;

	@Override
	public SFXViewField populatePanel(String scLabel, List<SFXViewListener> listListeners)
	{
		// Not used but needs to be created
		m_textValue = new SFXSwingText();
		
		m_comboUnits = new SFXSwingCombo();
		add(m_comboUnits);
		
		JLabel labelApply = new JLabel("Apply to");
		add(labelApply);
		
		SFXSwingRadio buttonDN = new SFXSwingRadio();
		buttonDN.createButton(1).setText("DN");
		buttonDN.addActionListener(this);
		add(buttonDN);
		
		m_textDN = new SFXSwingText();
		m_textDN.setColumns(SFXSwingText._COST);
		m_textDN.setEditable(false);
		m_textDN.setForeground(Color.RED);
		add(m_textDN);
		
		m_textFV = new SFXSwingText();
		m_textFV.setColumns(SFXSwingText._COST);
		m_textFV.setEditable(false);
		m_textFV.setForeground(Color.BLUE);
		add(m_textFV);
		
		SFXSwingRadio buttonFV = new SFXSwingRadio();
		buttonFV.createButton(-1).setText("FV");
		buttonFV.addActionListener(this);
		add(buttonFV);
		
		ButtonGroup group = new ButtonGroup();
		group.add(buttonDN);
		group.add(buttonFV);
		
		m_labelComment = new JLabel("Will @ DN ???");
		add(m_labelComment);
		
		return this;
	}	
}

class SFXSwingUnreal extends SFXSwingProportion
{
	private static final long serialVersionUID = 6344421740247273355L;

	private static final float _MINIMUM = 0.0f;
	private static final float _MAXIMUM = 1.0f;
	
	private ButtonGroup m_groupUnreal = null;
	
	@Override
	protected SFXViewField populatePanel(String scLabel, List<SFXViewListener> listListeners) 
	{
		String[] ascButtons = {"Real (unset)", "Difficult (Int DN 10)", "Average (Int DN 6)", "Easy (Int DN 0)"};
		m_groupUnreal = new ButtonGroup();

		for (int iIndex = 0; ascButtons.length>iIndex; ++iIndex)
		{
			SFXSwingRadio button = new SFXSwingRadio();
			button.createButton(iIndex * 25).setText(ascButtons[iIndex]);
			button.addActionListener(this);
			add(button);
			
			m_groupUnreal.add(button);
		}
		
		// Cost saving of being unreal
		m_textDN = new SFXSwingText();
		m_textDN.setColumns(SFXSwingText._COST);
		m_textDN.setEditable(false);
		add(m_textDN);
		
		// Slider is 0 to 1000 which corresponds to 100% DN / 0% FV to 0% DN / 100 %FV
		m_sliderFraction = new SFXSwingSlider();
		m_sliderFraction.setLower(_MINIMUM, "DN").setUpper(_MAXIMUM, "FV").setPaintLabels(true);
		m_sliderFraction.addChangeListener(this);
		add(m_sliderFraction);
		
		// Need to create but not used
		m_textValue = new SFXSwingText();
		
		return this;
	}
	
	@Override
	public SFXViewField setValue(SFXBase field)
	{
		SFXFractionUnreal fractionUnreal = (SFXFractionUnreal) field;
		int iDisbelief = fractionUnreal.getDisbelief();
		
		Enumeration<AbstractButton> enumButtons = m_groupUnreal.getElements();
		while (enumButtons.hasMoreElements())
		{
			SFXSwingRadio button = (SFXSwingRadio) enumButtons.nextElement();
			int iValue = button.getValue();
			button.setSelected(false);
			if (iValue!=iDisbelief) continue;
			button.setSelected(true);
		}
		
		int iEffectReduction = fractionUnreal.getCost();
		m_textValue.setText(Integer.toString(iEffectReduction));
		
		// SFXSwingSub sets the m_textValue to the value
		// SFXSwingScalar sets m_textDN to the cost
		// SFXSwingProportion will set m_sliderFraction to fractional value
		return super.setValue(field);
	}
	
	@Override
	protected SFXViewField actionControl(SFXSwingControl control, String scCommand)
	{
		if (control instanceof SFXSwingRadio)
		{
			scCommand = String.format("LEVEL %d", control.getValue());
			return modifyText(control, scCommand);
		}		
		return super.actionControl(control, scCommand);
	}	
}

class SFXSwingModifiers extends SFXSwingDouble
{
	private static final long serialVersionUID = -375347410096924692L;

	private Color m_colourDefault = null;

	@Override
	protected SFXViewField populatePanel(String scLabel, List<SFXViewListener> listListeners) 
	{
		super.populatePanel(scLabel, listListeners);
		m_colourDefault = m_textDN.getBackground();
		return this;
	}
	
	@Override
	public SFXViewField setValue(SFXBase field)
	{
		SFXFieldModifiers fieldModifiers = (SFXFieldModifiers) field;
		
		if (fieldModifiers.isLimited())
		{
			m_textDN.setBackground(Color.YELLOW);
			m_textFV.setBackground(Color.YELLOW);
		}
		else
		{
			m_textDN.setBackground(m_colourDefault);
			m_textFV.setBackground(m_colourDefault);			
		}
		
		return super.setValue(field);
	}
}

class SFXSwingSkill extends SFXSwingBase implements ActionListener
{
	private static final long serialVersionUID = 2909911637203396461L;

	private SFXSwingCombo m_comboUnits = null;
	private SFXSwingText m_textConditions = null;
	private int m_iCondition = 0;

	@Override
	protected SFXViewField populatePanel(String scLabel, List<SFXViewListener> listListeners) 
	{
		m_comboUnits = new SFXSwingCombo();
		add(m_comboUnits);
		
		m_textConditions = new SFXSwingText();
		m_textConditions.setColumns(SFXSwingText._LINE);
		m_textConditions.setEditable(false);
		add(m_textConditions);
		
		return this;		
	}	
	
	@Override
	public SFXViewField setValue(SFXBase field)
	{
		SFXValue value = (SFXValue)field;
			
		int iCount = m_comboUnits.getItemCount();
		
		if (0==iCount)		
		{
			String[] ascContents = value.getMultiplierContent();
			
			for (int iIndex = 0; ascContents.length>iIndex; ++iIndex)
				m_comboUnits.addItem(ascContents[iIndex]);
		}
		
		// Set the combo box index		
		int iMultiplierIndex = value.getMultiplierIndex();
		int iSelectedIndex = m_comboUnits.getSelectedIndex();
		
		if (m_iCondition!=iMultiplierIndex)
		{
			SFXSkill skillConditions = (SFXSkill)value.m_Multiplier;
			String[] ascConditions = skillConditions.getBasis(iMultiplierIndex);
	
			StringBuilder sb = new StringBuilder();
			
			if (0<ascConditions[0].length()) sb.append("Based on ");
			if (0<ascConditions[0].length()) sb.append(ascConditions[0]);
			
			if (0<ascConditions[1].length()) sb.append(" and reference result ");
			if (0<ascConditions[1].length()) sb.append(ascConditions[1]);
			
			if (0<ascConditions[2].length()) sb.append(" against target ");
			if (0<ascConditions[2].length()) sb.append(ascConditions[2]);
			
			m_iCondition = iMultiplierIndex;
			m_textConditions.setText(sb.toString());
		}
		
		if (iMultiplierIndex!=iSelectedIndex) m_comboUnits.setSelectedIndex(iMultiplierIndex);

		// Only listen to combo selection _after_ setting initial values because otherwise triggers value change before parents have their fields set which is a problem when loading existing data
		if (0==iCount) m_comboUnits.addActionListener(this);
		
		return super.setValue(field);
	}	
}

class SFXSwingContainer extends SFXSwingBase
{
	private static final long serialVersionUID = 2067364095882483986L;

	private List<SFXViewField> m_listContained = null;
	
	@Override
	public SFXViewField createPanel(String scLabel, List<SFXViewListener> listListeners)
	{
		m_listContained = new CopyOnWriteArrayList<SFXViewField>();
		return this;
	}
	
	@Override
	public SFXViewField setValue(SFXBase field)
	{
		for (SFXViewField swingField : m_listContained)
			swingField.setValue(field);
		return this;
	}
	
	public SFXSwingContainer setChild(SFXViewField fieldChild)
	{
		m_listContained.add(fieldChild);
		return this;
	}
}

class SFXSwingTimeFV extends SFXSwingSub
{
	private static final long serialVersionUID = -2294012435962001465L;

	private SFXTable m_timeTable = null;
	private SFXTime m_timeMultiplier = null;
	private SFXSwingText m_textUnits = null;
	
	@Override
	public SFXViewField createPanel(String scLabel, List<SFXViewListener> listListeners)
	{
		m_timeTable = SFXTable.getDefaultValueTable();
		m_timeMultiplier = new SFXTime();
		return super.createPanel(scLabel, listListeners);
	}
	
	@Override
	protected SFXViewField populatePanel(String scLabel, List<SFXViewListener> listListeners) 
	{
		// Populate like sub but change the colour to BLUE
		super.populatePanel(scLabel, listListeners);
		m_textValue.setForeground(Color.BLUE);

		m_textUnits = new SFXSwingText();
		m_textUnits.setColumns(SFXSwingText._COLUMNS);
		m_textUnits.setEditable(false);
		m_textUnits.setForeground(Color.BLUE);
		add(m_textUnits);
		
		return this;
	}
	
	@Override
	public SFXViewField setValue(SFXBase field)
	{
		SFXDouble valueModifier = (SFXDouble)field;
		
		String[] ascContent = m_timeMultiplier.getContent();
				
		int iValueFV = valueModifier.getValueFV();
		
		// First find the best multiplier to set units
		int iCount = 0;
		
		for (iCount = 1; iCount<ascContent.length; ++iCount)
		{
			int iOffset = m_timeMultiplier.setIndex(iCount).getOffset();
			if (iOffset>iValueFV) break;
		}
		
		iValueFV -= m_timeMultiplier.setIndex(iCount-1).getOffset();
		
		// Cross reference remaining cost on the table
		float fValue = m_timeTable.getValue(iValueFV);		
		m_textValue.setText(Float.toString(fValue));
		m_textUnits.setText(ascContent[iCount-1]);
		
		// Do not call super because field is not derived from SFXValue
		return this;
	}
}

class SFXSwingTextPane extends SFXSwingBase
{
	private static final long serialVersionUID = 5307844548901145294L;

	private JTextPane m_paneDescription = null;
	
	@Override
	public SFXViewField createPanel(String scLabel, List<SFXViewListener> listListeners)
	{
		m_listListeners = listListeners;
		
		m_paneDescription = new JTextPane();
		m_paneDescription.setEditable(false);
		m_paneDescription.setContentType("text/html");
		m_paneDescription.setText("<html><body bgcolor=#fffaec><p>Loading HTML ...</p></body></html>");
		
		JScrollPane scrollPane = new JScrollPane(m_paneDescription);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		
		setLayout(new BorderLayout());		
		add(scrollPane, BorderLayout.CENTER);
		
		return this;
	}
	
	@Override
	public SFXViewField setValue(SFXBase field)
	{
		String scContent = field.getValue();
		// Remove lots of whitespace
		scContent = scContent.replaceAll("\\r\\n", " ");
		m_paneDescription.setText(scContent);
		return this;
	}
	
	public SFXBase createContent(SFXModel dataModel, SFXModelListener listener, SFXViewQuery queryView)
	{
		SFXTextDescription descriptionData = new SFXTextDescription();
		descriptionData.createField("Description");		
		descriptionData.loadOutline(queryView, dataModel, "resources/Outline.html");
		descriptionData.insertChangePassing(listener, dataModel);
		return descriptionData;
	}
}
