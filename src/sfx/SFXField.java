package sfx;

import java.io.Serializable;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.CopyOnWriteArrayList;

public interface SFXField
{
	/* Fields have methods -
	 * getValue gets the current value
	 * setValue sets a new value for the field and propagates the change
	 * {either from an int value or a string which gives enough flexibility to include anything}
	 * updateValue sets the value
	 * propagateChange which ensures dependent values are also updated {via updateValue}
	 * changedField then informs listeners that the field has changed
	 * 
	 * So the whole life cycle goes something like this:
	 * Field A control sent by view
	 * Control 'setValue' of A 
	 * Model 'propagateChange' for A value
	 *    This 'updateValue' of B which is dependent
	 *    Model 'propagateChange' value for B
	 *    Model 'changedValue' for B to inform listeners of field change
	 *    View hears change B
	 *    View 'getValue' for B
	 * Model 'changedValue' for A
	 * View hears change A
	 * View 'getValue' for field A
	 */
	
	/** Get the field value as a string (so easily shown no matter what) */
	public String getValue();
	/** Set the field as a value {usually a delta from a button} */
	public SFXField setValue(int iValue);
	/** Set the field value from an text field so change as needed to value type */
	public SFXField setValue(String scValue);
	/** Update the value from dependencies */
	public SFXField updateValue();
	
	/** When value is changed then make sure dependent values are updated */
	public SFXField propagateChange();
	
	/** Supporting these are the method to send changes */
	public SFXField changedField();
}

/** Basic outline of a field provide skeleton implementations*/

class SFXBase implements SFXField, Serializable 
{
	private static final long serialVersionUID = 7360923345843001413L;

	protected String m_scField = "<Pending>";
	protected List<SFXBase> m_listDependents = null;
	private SFXBase m_fieldParent = null;

	protected transient List<SFXModelListener> m_listFieldListeners = null; // Listeners are transient so view is not also saved when fields are saved
	
	public SFXBase createField(String scField)
	{
		m_scField = scField;
		m_listDependents = new CopyOnWriteArrayList<SFXBase>();
		return this;
	}
	
	/**
	 * SFXField does not have individual list of listeners but uses a list held
	 * in common by the model. This means that when the field changes then 
	 * whoever is listening to the model has to know what field is changing and 
	 * respond appropriately.  this makes the listener management easier and 
	 * makes it easy to change the underlying model content 
	 */
	
	public SFXBase addListeners(List<SFXModelListener> listListeners)
	{
		m_listFieldListeners = listListeners;
		return this;
	}
	
	/* ======================================================================
	   Implement the interface functions so they link together
	   So setValue calls propagateChange which then executes changedField
	   ====================================================================== */
	
	@Override
	public String getValue()
	{
		return m_scField;
	}
	
	@Override
	public SFXField setValue(int iValue)
	{
		return propagateChange();
	}
	
	@Override
	public SFXField setValue(String scValue)
	{
		m_scField = scValue;
		return propagateChange();
	}
	
	@Override
	public SFXField updateValue()
	{
		return propagateChange();
	}
	
	@Override
	public SFXField changedField()
	{
		for (SFXModelListener listener : m_listFieldListeners) listener.changedField(this);
		return this;
	}
	
	/* ======================================================================
	   All fields need to provide DN 'cost'
	   ====================================================================== */
	
	public int getCost()
	{
		return 0;
	}
	
	/* ======================================================================
	   Handle the dependencies using a list of dependent values that are 
	   updated on change
	   ====================================================================== */
	   
	public SFXBase addDepends(SFXBase fieldDepends)
	{
		fieldDepends.m_listDependents.add(this);
		return this;
	}

	public SFXBase removeDepends(SFXBase fieldDepends)
	{
		fieldDepends.m_listDependents.remove(this);
		updateValue();
		return this;
	}
	
	@Override
	public SFXField propagateChange() 
	{
		//System.out.println(String.format("Propagate %s as %s", m_scField, getValue()));
		
		// Getting value forces recalculation of fields which then sets a new value
		for (SFXField field : m_listDependents)
			field.updateValue();
		return changedField();
	}
	
	/* ======================================================================
	   To support creating the specific fields have a factory method
	   ====================================================================== */

	protected SFXBase getInstance()
	{
		return this;
	}
	
	protected SFXBase getParent()
	{
		return m_fieldParent;
	}
	
	protected SFXBase setParent(SFXBase fieldParent)
	{
		m_fieldParent = fieldParent;
		return this;
	}
	
	@Override
	public String toString()
	{
		return m_scField;
	}
}

/** A scalar field has a value where the cost is simply the value 
 * The value set is a delta on the current value if set with a value
 * Or the content of a string if set explicitly
 */

class SFXScalar extends SFXBase
{
	private static final long serialVersionUID = -7721512797208921129L;
	
	protected int m_iValue = 0;
	
	@Override
	public SFXField setValue(int iValue) 
	{
		// Scalar field uses the parameter as a delta to the current value
		m_iValue += iValue;
		if (0>m_iValue) m_iValue = 0;
		return propagateChange();
	}
	
	@Override
	public SFXField setValue(String scValue) 
	{
		int iValue = Integer.parseInt(scValue);
		if (0>iValue) iValue = 0;
		m_iValue = iValue;
		return propagateChange();
	}
	
	@Override
	public String getValue()
	{
		return Integer.toString(m_iValue);
	}
	
	// Cost is simply the same as the value 
	 
	@Override
	public int getCost()
	{
		return m_iValue;
	}
	
	public int getIndex()
	{
		return m_iValue;		
	}
	
	/** getBonus is a placeholder for derived classes to package series of checks */
	
	public int getBonus()
	{
		return m_iValue;
	}

	@Override
	public String toString()
	{
		return String.format("%s (%s) (%d)", m_scField, getValue(), getCost());
	}
}

/** A calculated field is a simple value like the scalar but the value is just set */

class SFXCalculated extends SFXScalar
{
	private static final long serialVersionUID = 126238835181007922L;

	protected List<SFXBase> m_listSource = null;
	
	@Override
	public SFXBase createField(String scField)
	{
		m_listSource = new CopyOnWriteArrayList<SFXBase>();
		return super.createField(scField);
	}
	
	@Override
	public SFXField setValue(int iValue) 
	{
		// Calculated field just sets the value
		m_iValue = iValue;
		return propagateChange();
	}	
	
	@Override
	public SFXBase addDepends(SFXBase fieldDepends)
	{
		// Add source field to list held so have list of sources to recalculate value
		m_listSource.add(fieldDepends);
		return super.addDepends(fieldDepends);
	}

	@Override
	public SFXBase removeDepends(SFXBase fieldDepends)
	{
		m_listSource.remove(fieldDepends);
		fieldDepends.m_listDependents.remove(this);
		return this;
	}
	
	@Override
	public String toString()
	{
		return String.format("%s (%d)", m_scField, m_iValue);
	}
}

/** A double value has separate calculate values for DN and FV */

class SFXDouble extends SFXCalculated
{
	private static final long serialVersionUID = 5372085204285961982L;

	protected int m_iValueDN = 0;
	protected int m_iValueFV = 0;
	
	@Override
	public SFXField setValue(String scValue)
	{
		if (scValue.startsWith("DN"))
		{
			int iValueDN = Integer.parseInt(scValue.substring(3));
			m_iValueDN = iValueDN;
		}
		
		if (scValue.startsWith("FV"))
		{
			int iValueFV = Integer.parseInt(scValue.substring(3));
			m_iValueFV = iValueFV;
		}
		
		return propagateChange();
	}
	
	@Override
	public int getCost()
	{
		return m_iValueDN + m_iValueFV;
	}
	
	public int getValueDN()
	{
		return m_iValueDN;
	}
	
	public int getValueFV()
	{
		return m_iValueFV;
	}
	
	@Override
	public String toString()
	{
		return String.format("%s (%d/%d)", m_scField, m_iValueDN, m_iValueFV);
	}
}

/** A fractional field has a float value and includes a default which sets the fraction to a specific value */

class SFXFraction extends SFXBase
{
	private static final long serialVersionUID = -1382929882032852833L;

	private float m_fValue = 0.5f;
	private float m_fDefault = 0.5f;
	private boolean m_zDefault = false;
	
	@Override
	public String getValue()
	{
		return Float.toString(m_fValue);
	}
	
	@Override
	public SFXField setValue(String scValue)
	{
		if (scValue.startsWith("DEFAULT"))
		{
			m_zDefault = true;
			m_fValue = m_fDefault;
			return propagateChange();
		}
		
		if (scValue.startsWith("FRACTION"))
		{
			m_zDefault = false;
			return propagateChange();
		}
		
		float fValue = Float.parseFloat(scValue);
		if (m_zDefault) fValue = m_fDefault;
		m_fValue = fValue;
		
		return propagateChange();
	}
	
	/** getCost applies the fraction to a given int and rounds to the nearest int
	 * Be careful when the fraction is exactly 0.5 since could end up having sum fractions adding to the value + 1
	 * e.g. 1/2 * 11 rounded up is 6; so a split of 50/50 gives 6 and 6 instead of 5 and 6 
	 * Handle this by using the first fraction and then total - first for the second */
	
	public int getCost(int iValue)
	{
		float fReturn = m_fValue * iValue;
		fReturn += 0.5;
		return (int)fReturn;
	}

	@Override
	public String toString()
	{
		return String.format("%s (%s) (%d/%d)", m_scField, getValue(), getCost(100), 100-getCost(100));
	}
}

/** A value field has a difference between the value and the cost and also includes a cost multiplier */

class SFXValue extends SFXScalar
{
	private static final long serialVersionUID = -6816232194531430215L;

	protected SFXTable m_Table = null;
	protected SFXTableMultiplier m_Multiplier = null;
	
	/** Value obtained from table entry
	 * The value is used for the table entry
	 */
	
	@Override
	public String getValue()
	{
		float fValue = m_Table.getValue(m_iValue);
		return Float.toString(fValue);
	}

	/** Setting the value must take into account the table range */
	
	@Override
	public SFXField setValue(int iValue) 
	{
		// Test the change first
		iValue += m_iValue;
		if (m_Table.outRange(iValue)) return this;
		m_iValue = iValue;
		
		return propagateChange();
	}
	
	/** When the value is directly edited the nearest entry is used */
	
	@Override
	public SFXField setValue(String scValue) 
	{
		float fValue = Float.parseFloat(scValue);
		int iValue = m_Table.closestValue(fValue);
		if (m_Table.outRange(iValue)) return this;
		m_iValue = iValue;
		
		return propagateChange();
	}
	
	/** The cost is obtained from the table entry AND the cost multiplier */
	
	@Override
	public int getCost() 
	{
		return m_Multiplier.getOffset() + m_Table.getCost(m_iValue);
	}
	
	/* ======================================================================
	   The field chooses the value {and cost} from a table
	   The value of the field is the entry in the table and 
	   There is a multiplier table which has separate index
	   ====================================================================== */
	   
	public SFXValue setTable(SFXTable table) 
	{
		m_Table = table;
		return this;
	}
	
	public String[] getTableContent()
	{
		return m_Table.getContent();
	}
	
	public SFXField setMultiplierTable(SFXTableMultiplier multiplier) 
	{
		m_Multiplier = multiplier;
		return propagateChange();
	}
	
	public String getUnit()
	{
		return m_Multiplier.getName();
	}
	
	public String[] getMultiplierContent()
	{
		return m_Multiplier.getContent();
	}
	

	public int getMultiplierIndex() 
	{
		return m_Multiplier.getIndex();
	}
	
	public SFXField setMultiplierIndex(int iMultiplier) 
	{
		m_Multiplier.setIndex(iMultiplier);
		return propagateChange();
	}
	
	@Override
	public String toString()
	{
		return String.format("%s (%s %s) (%d)", m_scField, getValue(), getUnit(), getCost());
	}
}

/** A value modifier is the same as a value but there is a boolean to determine if
 * the resulting cost is reduced or the value provided a reduced FV 
 */

class SFXValueModifier extends SFXValue
{
	private static final long serialVersionUID = 6945256113366728817L;

	protected boolean m_zReduce = true;
	protected String m_scComment = null;
	
	@Override
	public SFXBase createField(String scField)
	{
		m_scComment = "<None>";
		return super.createField(scField);
	}
	
	@Override
	public SFXField setValue(String scValue)
	{
		if (scValue.startsWith("DN")) return setApplication(true);
		if (scValue.startsWith("FV")) return setApplication(false);
		return super.setValue(scValue);
	}
	
	@Override
	public int getCost() 
	{
		if (m_zReduce) return super.getCost();
		return 0;
	}
	
	public boolean getApplication()
	{
		return m_zReduce;
	}
	
	private SFXField setApplication(boolean zApply)
	{
		m_zReduce = zApply;
		// Propagate value
		return propagateChange();
	}
	
	public int getReduction()
	{
		if (!m_zReduce) return super.getCost();
		return 0;		
	}
	
	public String getComment()
	{
		return m_scComment;
	}
}

/** A value follower has a boolean flag to indicate if the value of this field follows another field
 * When the flag is set the value of the field and the table multiplier match the followed field and
 * any cost calculated takes into account the difference between the followed field and this */

class SFXValueFollows extends SFXValue
{
	private static final long serialVersionUID = 3678015129165791850L;

	protected SFXScalar m_follows = null;
	private boolean m_zDefault = true;
	
	@Override
	public SFXField setValue(int iValue) 
	{
		if (m_zDefault) 
			iValue = m_follows.m_iValue;
		else
			iValue += m_iValue;
		
		if (m_Table.outRange(iValue)) return this;
		m_iValue = iValue;
		
		return propagateChange();
	}
	
	@Override
	public SFXField setValue(String scValue) 
	{
		// Check for change to the set default following
		if (scValue.startsWith("ENABLE")) return setDefault(true);
		if (scValue.startsWith("DISABLE")) return setDefault(false);
		// Set value as for Value
		return super.setValue(scValue);
	}
	
	@Override
	public SFXField updateValue()
	{
		if (m_zDefault)
		{
			int iValue = m_follows.m_iValue;
			return setValue(iValue);
		}
		
		return this;
	}			

	@Override
	public int getCost() 
	{
		if (m_zDefault)
		{
			int iFollows = m_follows.getCost();
			int iCost = super.getCost();
			return iCost - iFollows;
		}
		return super.getCost();
	}
	
	public SFXValueFollows setFollows(SFXScalar follows) 
	{
		m_follows = follows;
		return this;
	}
	
	public boolean getDefault()
	{
		return m_zDefault;
	}
	
	protected SFXField setDefault(boolean zDefault)
	{
		m_zDefault = zDefault;
		return setValue(0);
	}
}

class SFXCompositeAction implements Serializable
{
	private static final long serialVersionUID = -6458199870896539559L;

	public SFXBase performAction(SFXBase field)
	{
		return field;
	}
};

class SFXComposite extends SFXBase
{
	private static final long serialVersionUID = -442406473146546941L;
	
	private List<SFXBase> m_listFields = null;
	
	public SFXCompositeAction performAction(SFXCompositeAction action)
	{
		for (SFXBase field : m_listFields)
		{
			action.performAction(field);
		}
		return action;
	}
	
	@Override
	public SFXBase createField(String scField)
	{
		super.createField(scField);
		m_listFields = new CopyOnWriteArrayList<SFXBase>();
		return this;
	}
	
	@Override
	public int getCost()
	{
		int iCost = 0;
		for (SFXBase field : m_listFields) iCost += field.getCost();
		return iCost;		
	}
	
	public SFXBase addField(SFXBase field)
	{
		// Makes parent dependent on children
		addDepends(field);
		// Sets the child parent
		field.setParent(this);
		// Adds the child to the parent list of children 
		m_listFields.add(field);
		// Sets the listeners for the child
		field.addListeners(m_listFieldListeners);
		
		for (SFXModelListener listener : m_listFieldListeners)
			listener.addField(field);
		
		return this;
	}
	
	public SFXBase getField(int iField)
	{
		return m_listFields.get(iField);
	}
	
	public SFXBase removeField(SFXBase field)
	{
		// Tell the listeners that the field has been removed
		for (SFXModelListener listener : m_listFieldListeners)
			listener.removeField(field);
		
		m_listFields.remove(field);
		removeDepends(field);
		return this;
	}

	public List<SFXBase> getComposition() 
	{
		return m_listFields;
	}
}

/* ==========================================================================
   Lots of specializations of the above with extra fields or different
   calculations 
   ==========================================================================*/

class SFXFieldSubMandatory extends SFXCalculated
{
	private static final long serialVersionUID = -6309685187854034573L;

	@Override
	public SFXField updateValue()
	{
		int iPoints = getCalculated();
		return setValue(iPoints);
	}	
	
	protected int getCalculated()
	{
		int iPoints = 0;
		
		for (SFXBase field : m_listSource)
			iPoints += field.getCost();
				
		return iPoints;		
	}
}

class SFXFieldHalfMandatory extends SFXFieldSubMandatory
{
	private static final long serialVersionUID = -6309685187854034573L;

	@Override
	public SFXField updateValue()
	{
		// Round down?
		int iPoints = getCalculated() / 2;
		return setValue(iPoints);
	}
}

class SFXFieldMandatory extends SFXCalculated
{
	private static final long serialVersionUID = -3772389142237131168L;

	@Override
	public SFXField updateValue()
	{
		ListIterator<SFXBase> iterateSources = m_listSource.listIterator();
		
		int iMandatory = iterateSources.next().getCost();
		int iCasting = iterateSources.next().getCost();
		int iHalf = iterateSources.next().getCost();
		
		int iLimited = iMandatory - iCasting;
		iLimited = iLimited<iHalf?iHalf:iLimited;
		
		return setValue(iLimited);
	}	
}

class SFXCompositeArea extends SFXComposite
{
	private static final long serialVersionUID = 596021631497381064L;

	//public SFXField createField(String scField, List<SFXModelListener> listListeners, SFXTable tableDefault)
	public SFXBase addFirst(SFXTable tableDefault)
	{		
		SFXFieldArea scalarArea = new SFXFieldArea();
		scalarArea.createField("Area").addListeners(m_listFieldListeners);
		addField(scalarArea);
		
		SFXFieldVolume scalarVolume = new SFXFieldVolume();
		scalarVolume.createField("Volume").addListeners(m_listFieldListeners);
		addField(scalarVolume);
		
		SFXFieldShape valueShape = new SFXFieldShape();
		valueShape.createField("Shape").addListeners(m_listFieldListeners);
		valueShape.setTable(tableDefault).setMultiplierTable(new SFXShape()).setValue(0);
		addField(valueShape);
		
		scalarArea.addDepends(scalarVolume).addDepends(valueShape);
		scalarVolume.addDepends(scalarArea).addDepends(valueShape);
		valueShape.addDepends(scalarArea).addDepends(scalarVolume);
		
		// Set dependency on contained fields so other fields can be dependent on composite
		addDepends(scalarArea).addDepends(scalarVolume).addDepends(valueShape);
		
		return this;
	}
}

class SFXFieldArea extends SFXScalar
{
	private static final long serialVersionUID = -840784669556433604L;
	
	@Override
	public SFXField updateValue()
	{
		ListIterator<SFXBase> iterateOthers = m_listDependents.listIterator();
		
		int iSpace = iterateOthers.next().getCost();
		int iShape = iterateOthers.next().getCost();
		
		if (0<(iSpace+iShape))
		{
			m_iValue = 0;
			return changedField();
		}
		return this;
	}
	
	@Override
	public int getCost()
	{
		return m_iValue * 2;
	}	
}

class SFXFieldVolume extends SFXScalar
{
	private static final long serialVersionUID = 6477600136077997550L;

	@Override
	public SFXField updateValue()
	{
		ListIterator<SFXBase> iterateOthers = m_listDependents.listIterator();
		
		int iArea = iterateOthers.next().getCost();
		int iShape = iterateOthers.next().getCost();
		
		if (0<(iArea+iShape))
		{
			m_iValue = 0;
			return changedField();
		}
		return this;
	}
	
	@Override
	public int getCost()
	{
		return m_iValue * 5;
	}	
}

class SFXFieldShape extends SFXValue
{
	private static final long serialVersionUID = -5528531502147076632L;

	@Override
	public SFXField updateValue()
	{
		ListIterator<SFXBase> iterateOthers = m_listDependents.listIterator();
		
		int iArea = iterateOthers.next().getCost();
		int iSpace = iterateOthers.next().getCost();
		
		if (0<(iArea+iSpace))
		{
			m_iValue = 0;
			return changedField();
		}
		return this;
	}	
}

class SFXFieldMultiTarget extends SFXScalar
{
	private static final long serialVersionUID = -8600968789400727599L;
	
	private SFXCompositeArea m_fieldArea = null;

	@Override
	public SFXField setValue(int iValue) 
	{
		// Scalar field uses the parameter as a delta to the current value
		m_iValue += iValue;
		if (1==m_iValue) m_iValue += iValue;
		if (0>m_iValue) m_iValue = 0;
		return propagateChange();
	}
	
	@Override
	public SFXField setValue(String scValue) 
	{
		int iValue = Integer.parseInt(scValue);
		if (1==iValue) iValue = m_iValue>1?0:2;
		m_iValue = iValue;
		if (0>m_iValue) m_iValue = 0;
		return propagateChange();
	}
	
	public SFXBase setSource(SFXCompositeArea fieldArea)
	{
		m_fieldArea = fieldArea;
		return this;
	}
	
	@Override
	public int getCost()
	{
		int iArea = m_fieldArea.getCost();
		if (0<iArea) return m_iValue * 6; // Cost doubles if area of effect too
		return m_iValue * 3;
	}						
}

class SFXMultiAttribute extends SFXTableMultiplier
{
	private static final long serialVersionUID = -7807733172775605647L;

	private static final int[] sm_aiShapeOffsets = {0, 3, 6, 9, 12, 15, 18, 21};
	private static final String[] sm_scShapeNames = {"unset", "TWO attributes", "three", "four", "five", "six", "seven", "ALL"};
	
	public SFXMultiAttribute()
	{
		m_aiOffsets = sm_aiShapeOffsets;
		m_scNames = sm_scShapeNames;
	}
}

class SFXFieldChangeTarget extends SFXScalar
{
	private static final long serialVersionUID = -5972135559134091324L;

	private SFXCompositeArea m_fieldArea = null;
	private SFXScalar m_scalarTarget = null;
	private SFXValue m_valueAttributes = null;
	
	@Override
	public SFXField setValue(int iValue) 
	{
		// Scalar field uses the parameter as a delta to the current value
		m_iValue += iValue;
		return limitValue();
	}
	
	@Override
	public SFXField setValue(String scValue) 
	{
		int iValue = Integer.parseInt(scValue);
		m_iValue = iValue;
		return limitValue();
	}
	
	@Override
	public SFXField updateValue()
	{
		return limitValue();
	}
	
	private SFXField limitValue()
	{
		int iTargets = m_scalarTarget.m_iValue;
		if (iTargets==0) iTargets = 1;
		if (0>m_iValue) m_iValue = 0;
		m_iValue = iTargets<m_iValue?iTargets:m_iValue;
		return propagateChange();						
	}
	
	public SFXBase setSources(SFXCompositeArea fieldArea, SFXScalar scalarTarget, SFXValue valueAttributes)
	{
		m_fieldArea = fieldArea;
		m_scalarTarget = scalarTarget;
		m_valueAttributes = valueAttributes;
		return this;
	}
	
	@Override
	public int getCost()
	{
		double dfMultiplier = 1.0;
		
		int iArea = m_fieldArea.getCost();
		if (0<iArea) dfMultiplier += 0.5;
		
		int iAttribute = m_valueAttributes.getCost();
		if (0<iAttribute) dfMultiplier += 0.5;
		
		// Cost 5 points per target change but x1.5 if area effect or multiple attributes or x2 if both!
		double dfValue = m_iValue * 5 * dfMultiplier;
		return (int)Math.ceil(dfValue);
	}						
}

class SFXFieldVariableEffect extends SFXScalar
{
	private static final long serialVersionUID = 1L;

	@Override
	public int getCost()
	{
		return m_iValue * 2;
	}							
}

class SFXFieldVariableDuration extends SFXComposite
{
	private static final long serialVersionUID = 5859327260350076191L;
	
	@Override
	public SFXBase createField(String scField)
	{
		super.createField(scField);
		
		SFXValue valueSwitch = new SFXValue();
		valueSwitch.createField("Switch").addListeners(m_listFieldListeners);
		valueSwitch.setTable(SFXTable.getDefaultValueTable()).setMultiplierTable(new SFXSwitch()).setValue(0);
		addField(valueSwitch);
		
		SFXValue valueShape = new SFXValue();
		valueShape.createField("Extend").addListeners(m_listFieldListeners);
		valueShape.setTable(SFXTable.getDefaultValueTable()).setMultiplierTable(new SFXTime()).setValue(0);
		addField(valueShape);
		
		// Set dependency on contained fields so other fields can be dependent on composite
		addDepends(valueSwitch).addDepends(valueShape);
		
		return this;
	}
}

class SFXFieldApportation extends SFXComposite
{
	private static final long serialVersionUID = -1270174766942708819L;

	public static final int _MOVE = 1;

	@Override
	public SFXBase createField(String scField)
	{
		super.createField(scField);
		
		SFXScalar valueToHit = new SFXScalarToHit();
		valueToHit.createField("ToHit").addListeners(m_listFieldListeners);
		addField(valueToHit);
		
		SFXValueMove valueMove = new SFXValueMove();
		valueMove.createField("Move").addListeners(m_listFieldListeners);
		valueMove.setTable(SFXTable.getDefaultValueTable()).setMultiplierTable(new SFXWeight());
		valueMove.setDefault(false);
		addField(valueMove);
		
		// Set dependency on contained fields so other fields can be dependent on composite
		addDepends(valueToHit).addDepends(valueMove);
		
		return this;
	}	
}

class SFXScalarToHit extends SFXScalar
{
	private static final long serialVersionUID = 4746643931173795742L;

	@Override
	public int getCost()
	{
		return m_iValue * 2;
	}
}

class SFXValueMove extends SFXValueFollows
{
	private static final long serialVersionUID = -5472123173116604843L;
	private List<SFXBase> m_listSource = null;
	private SFXSpeed m_tableSpeed = null;
	
	@Override
	public SFXBase createField(String scField)
	{
		m_listSource = new CopyOnWriteArrayList<SFXBase>();
		m_tableSpeed = new SFXSpeed();
		return super.createField(scField);
	}
	
	@Override
	public SFXField setValue(int iValue)
	{
		// Derive from follows (because want default switch) but want to use value independently
		iValue += m_iValue;
		if (m_Table.outRange(iValue)) return this;
		m_iValue = iValue;
		return propagateChange();
	}
	
	@Override
	public SFXField updateValue()
	{
		// Does not follow the effect field - value not changed though cost will so propogate change
		return propagateChange();
	}
	
	@Override
	public int getCost()
	{
		if (!getDefault()) return 0;
		
		int iCost = 0;
		for (SFXBase fieldCost : m_listSource)
			iCost += fieldCost.getCost();
		iCost = (iCost + 9) / 10;
		return iCost;
	}
	
	@Override
	public SFXBase addDepends(SFXBase fieldDepends)
	{
		m_listSource.add(fieldDepends);
		return super.addDepends(fieldDepends);
	}
	
	public float getMass()
	{
		return m_Table.getValue(m_iValue);
	}

	public String[] getMultiplierContentEx() 
	{
		return m_tableSpeed.getContent();
	}
	
	public int getDeltaEffectMassCost(float fTry, int iOffsetMass) 
	{
		int iMassCost = m_Table.closestValue(fTry);
		iMassCost += m_Multiplier.setIndex(iOffsetMass).getOffset();
		int iEffectCost = m_follows.getCost();
		
		return iEffectCost - iMassCost;
	}

	public float getMoveCost(int iDeltaEffectMassCost, int iOffsetMove) 
	{
		int iBaseCost = m_tableSpeed.setIndex(iOffsetMove).getOffset();
		if (iBaseCost>iDeltaEffectMassCost) return -1;
		if (m_Table.outRange(iDeltaEffectMassCost - iBaseCost)) return -2;
		return m_Table.getValue(iDeltaEffectMassCost - iBaseCost);
	}
}

class SFXFieldMaintenance extends SFXCalculated
{
	private static final long serialVersionUID = 7832854151579801479L;
	
	@Override
	public int getCost()
	{
		if (0==m_iValue) return 0;
		
		int iCost = 0;
		
		for (SFXBase field : m_listSource)
			iCost += field.getCost();

		return 10 * iCost / 100;
	}	
}

class SFXFieldFocus extends SFXCalculated
{
	private static final long serialVersionUID = 7086258324490475046L;

	@Override
	public int getCost()
	{
		if (0==m_iValue) return 0;
		
		int iCost = 0;
		
		for (SFXBase fieldCost : m_listSource)
			iCost += fieldCost.getCost();

		iCost = (iCost + 4)/5; // Round up
			
		return iCost;
	}
}

class SFXFieldCharges extends SFXScalar
{
	private static final long serialVersionUID = -2997670084141519123L;
	
	private boolean m_zWard = false;

	@Override
	public SFXField setValue(int iValue) 
	{
		m_iValue += iValue;
		// Round to nearest 5 charges
		m_iValue = m_iValue - (m_iValue % 5);
		if (0>m_iValue) m_iValue = 0;
		return propagateChange();
	}
	
	@Override
	public SFXField setValue(String scValue) 
	{
		// Format is %f %z
		String[] ascValues = scValue.split(" ");
		float fValue = Float.parseFloat(ascValues[0]);
		boolean zWard = Boolean.parseBoolean(ascValues[1]);
		m_iValue = (int)fValue;
		m_iValue = m_iValue - (m_iValue % 5);
		if (0>m_iValue) m_iValue = 0;
		return setWard(zWard).propagateChange();
	}
	
	@Override
	public int getCost()
	{
		// Ward is +10%
		if (m_zWard) return m_iValue * 2 * 110 / 100;
		return m_iValue * 2; // 5 charges = 10 pts
	}
	
	@Override
	public int getBonus()
	{
		if (m_zWard) return 1;
		return 0;
	}
	
	private SFXBase setWard(boolean zWard)
	{
		m_zWard = zWard;
		return this;
	}
}

class SFXFieldOptional extends SFXCalculated
{
	private static final long serialVersionUID = -2948309297813407739L;

	@Override
	public SFXField updateValue()
	{
		int iCost = 0;
		for (SFXBase field : m_listSource)
			iCost += field.getCost();
					
		return setValue(iCost);
	}
}

class SFXCompositeCommunity extends SFXComposite
{
	private static final long serialVersionUID = -158446178605259575L;

	// Community has a different table {even though the entries are the same} so the labels can be different
	private SFXTable m_tableCommunity = null;
	
	@Override
	public SFXBase createField(String scField)
	{
		m_tableCommunity = new SFXTableCommunity();
		m_tableCommunity.setInitialValues(32);	
		return super.createField(scField);
	}
	
	public SFXBase addFirst(SFXFieldCommunity field) 
	{
		field.setInitial(true);
		return addField(field);
	}
	
	@Override
	public SFXBase addField(SFXBase field) 
	{
		SFXFieldCommunity valueCommunity = (SFXFieldCommunity) field;
		valueCommunity.createField("Community").addListeners(m_listFieldListeners);
		valueCommunity.setTable(m_tableCommunity).setMultiplierTable(new SFXCommunityModifier()).setValue(0);
		return super.addField(field);
	}	
}

class SFXFieldCommunity extends SFXValueModifier
{
	private static final long serialVersionUID = 8633519908829467665L;
	
	private boolean m_zInitial = false;
	
	@Override
	public String getValue()
	{
		// Want the table index as from the field instead of the table value for that index
		return Integer.toString(m_iValue);
	}
	
	@Override
	public SFXField setValue(int iValue) 
	{
		// Want to just set the table index
		if (m_Table.outRange(iValue)) return this;
		m_iValue = iValue;
		return propagateChange();
	}
	
	@Override
	public int getCost() 
	{
		if (!m_zReduce) return 0;
		int iSize = m_Table.getCost(m_iValue);
		int iDN = m_Multiplier.getOffset();
		return iSize * iDN / 2;
	}
	
	@Override
	public int getReduction()
	{
		if (m_zReduce) return 0;
		return m_Table.getCost(m_iValue) * m_Multiplier.getOffset() / 2;
	}
	
	public boolean isInitial()
	{
		return m_zInitial;
	}
	
	public SFXBase setInitial(boolean zInitial)
	{
		m_zInitial = zInitial;
		return this;
	}
}

class SFXCompositeComponents extends SFXComposite
{
	private static final long serialVersionUID = 1722235361343889718L;

	private SFXBonus m_tableBonus = null;
	
	private List<SFXBase> m_listSource = null;
	private SFXCompositeActionReduction m_actionReduction = null;
	private SFXCompositeActionChanged m_actionChanged = null;
	private boolean m_zLimited = false;
	
	@Override
	public SFXBase createField(String scField)
	{
		m_tableBonus = new SFXBonus();
		m_tableBonus.setInitialValues(4);
		
		m_listSource = new CopyOnWriteArrayList<SFXBase>();
		m_actionReduction = new SFXCompositeActionReduction();
		m_actionChanged = new SFXCompositeActionChanged();
		return super.createField(scField);
	}

	public SFXBase addFirst(SFXFieldCommunity field) 
	{
		field.setInitial(true);
		return addField(field);
	}
	
	@Override
	public SFXBase addField(SFXBase field) 
	{
		SFXFieldComponent valueCommunity = (SFXFieldComponent) field;
		valueCommunity.createField("Component").addListeners(m_listFieldListeners);
		valueCommunity.setTable(m_tableBonus).setMultiplierTable(new SFXComponent()).setValue(0);
		return super.addField(field);
	}
	
	@Override
	public SFXBase addDepends(SFXBase field)
	{
		m_listSource.add(field);
		return super.addDepends(field);		
	}
	
	@Override
	public SFXField updateValue()
	{
		int iIndex = 0;
		
		// Sum of mandatory and optional elements
		int iLimitDN = m_listSource.get(iIndex++).getCost();
		iLimitDN += m_listSource.get(iIndex++).getCost();

		// Fraction split to limits
		SFXFraction fraction = (SFXFraction) m_listSource.get(iIndex++);
		int iLimitFV = iLimitDN - fraction.getCost(iLimitDN);
		iLimitDN = iLimitDN - iLimitFV;
				
		iLimitDN = (iLimitDN + 1) / 2;
		iLimitFV = (iLimitFV + 1) / 2;
		
		int iLimit = Math.min(iLimitDN, iLimitFV);
		
		int iReduceDN = getCost();
		int iReduceFV = m_actionReduction.getReduction(this);
		
		m_zLimited = false;
		if (iReduceDN>iLimit) m_zLimited = true;
		if (iReduceFV>iLimit) m_zLimited = true;
		
		// Need to inform listeners of the contained fields that the value has changed {to update their colour}
		m_actionChanged.changedField(this);
		
		return propagateChange();
	}
	
	public boolean isLimited()
	{
		return m_zLimited;
	}	
}

class SFXFieldComponent extends SFXFieldCommunity
{
	private static final long serialVersionUID = -4438902496743547464L;

	private boolean m_zDestroyed = false;
	
	@Override
	public SFXField setValue(String scValue) 
	{
		// Various values multiplexed into string
		if (scValue.startsWith("DESTROY")) return setDestroyed(true);
		if (scValue.startsWith("PRESERVE")) return setDestroyed(false);
		
		// Let super handle the DN/FV
		return super.setValue(scValue);
	}
	
	@Override
	public int getCost()
	{
		if (!m_zReduce) return 0;
		if (m_zDestroyed) return getCombined() * 2;
		return getCombined();
	}
	
	@Override
	public int getReduction()
	{
		if (m_zReduce) return 0;
		if (m_zDestroyed) return getCombined() * 2;
		return getCombined();
	}
	
	private int getCombined()
	{
		return m_iValue + m_Multiplier.getIndex();
	}
	
	@Override
	public int getBonus()
	{
		if (m_zDestroyed) return 1;
		return 0;
	}
	
	private SFXField setDestroyed(boolean zDestroyed)
	{
		m_zDestroyed = zDestroyed;
		return propagateChange();
	}	
}

class SFXCompositeActionReduction extends SFXCompositeAction
{
	private static final long serialVersionUID = -3427342354081369058L;

	private int m_iReduction = 0;
	
	public int getReduction(SFXComposite composite) 
	{
		m_iReduction = 0;
		composite.performAction(this);
		return m_iReduction;
	}
	
	@Override
	public SFXBase performAction(SFXBase field)
	{
		SFXFieldCommunity community = (SFXFieldCommunity) field;
		m_iReduction += community.getReduction();
		return field;
	}
}

class SFXCompositeActionChanged extends SFXCompositeAction
{
	private static final long serialVersionUID = -6143128705744700002L;

	public void changedField(SFXComposite composite)
	{
		composite.performAction(this);
		return;		
	}
	
	@Override
	public SFXBase performAction(SFXBase field)
	{
		field.changedField();
		return field;
	}
}

class SFXFieldConcentration extends SFXValueModifier
{
	private static final long serialVersionUID = -2218780497272620699L;

	@Override
	public SFXField propagateChange()
	{
		int iCost = super.getCost() + super.getReduction();
		
		if (0==m_iValue) 
			m_scComment = "None";
		else
			m_scComment = String.format("Will @ DN %d", iCost / 3 + 6);
		
		return super.propagateChange();
	}
	
	@Override
	public int getCost() 
	{
		if (m_zReduce) return super.getCost() / 3;
		return 0;
	}
	
	@Override
	public int getReduction()
	{
		if (!m_zReduce) return super.getReduction() / 3;
		return 0;		
	}						
}

class SFXCompositeGestures extends SFXComposite
{
	private static final long serialVersionUID = 2104314106820988193L;

	private SFXBonus m_tableBonus = null;
	
	@Override
	public SFXBase createField(String scField)
	{
		m_tableBonus = new SFXBonus();
		m_tableBonus.setInitialValues(4);
		return super.createField(scField);
	}

	public SFXBase addFirst(SFXFieldCommunity field) 
	{
		field.setInitial(true);
		return addField(field);
	}
	
	@Override
	public SFXBase addField(SFXBase field) 
	{
		SFXFieldGesture valueGesture = (SFXFieldGesture) field;
		valueGesture.createField("Gesture").addListeners(m_listFieldListeners);
		valueGesture.setTable(m_tableBonus).setMultiplierTable(new SFXGesture()).setValue(0);
		return super.addField(field);
	}
}

class SFXFieldGesture extends SFXFieldCommunity
{
	private static final long serialVersionUID = 6633630702232475080L;

	@Override
	public SFXField propagateChange()
	{
		int iCost = m_Multiplier.getIndex();
		
		if (2>iCost) 
			m_scComment = "None";
		else
			m_scComment = String.format("Dex @ DN %d", (iCost - 2) * 4 + 8);
		
		return super.propagateChange();
	}
	
	@Override
	public int getCost()
	{
		if (!m_zReduce) return 0;
		return m_iValue + m_Multiplier.getIndex();
	}
	
	@Override
	public int getReduction()
	{
		if (m_zReduce) return 0;
		return m_iValue + m_Multiplier.getIndex();
	}	
}

class SFXFieldIncantation extends SFXValueModifier
{
	private static final long serialVersionUID = -3584900809489812628L;
	
	public static final int _FOREIGN = 1;
	public static final int _LOUD = 2;
	public static final int _PROFANE = 4;

	private boolean m_zForeign = false;
	private boolean m_zLoud = false;
	private boolean m_zProfane = false;
	
	@Override
	public SFXField setValue(String scValue)
	{
		if (scValue.startsWith("FOREIGN")) return setForeign(true);
		if (scValue.startsWith("NATIVE")) return setForeign(false);
		if (scValue.startsWith("LOUD")) return setLoud(true);
		if (scValue.startsWith("SOFT")) return setLoud(false);
		if (scValue.startsWith("PROFANE")) return setProfane(true);
		if (scValue.startsWith("POLITE")) return setProfane(false);
		
		return super.setValue(scValue);	
	}
	
	@Override
	public SFXField propagateChange()
	{
		int iCost = m_Multiplier.getIndex();
		
		if (3>iCost) 
			m_scComment = "None";
		else
			m_scComment = String.format("Mind @ DN %d", (iCost - 3) * 4 + 8);
		
		return super.propagateChange();
	}
	
	@Override
	public int getCost() 
	{
		if (!m_zReduce) return 0;
		return getCombined();
	}
	
	@Override
	public int getReduction()
	{
		if (m_zReduce) return 0;
		return getCombined();		
	}
	
	private int getCombined()
	{
		int iCost = super.getCost() + super.getReduction();
		if (0==iCost) return 0;
		if (m_zForeign) ++iCost;
		if (m_zLoud) ++iCost;
		if (m_zProfane) ++iCost;
		return iCost;		
	}
	
	@Override
	public int getBonus()
	{
		int iBonus = 0;
		if (m_zForeign) iBonus += 1;
		if (m_zLoud) iBonus += 2;
		if (m_zProfane) iBonus += 4;
		return iBonus;	
	}
	
	private SFXField setForeign(boolean zForeign)
	{
		m_zForeign = zForeign;
		return propagateChange();
	}
	
	private SFXField setLoud(boolean zLoud)
	{
		m_zLoud = zLoud;
		return propagateChange();
	}
	
	private SFXField setProfane(boolean zProfane)
	{
		m_zProfane = zProfane;
		return propagateChange();
	}
}

class SFXFieldRelatedSkill extends SFXValueModifier
{
	private static final long serialVersionUID = 2781374619703337341L;

	@Override
	public SFXField propagateChange()
	{
		int iCost = m_Multiplier.getIndex();
		
		if (1>iCost) 
			m_scComment = "None";
		else
			m_scComment = String.format("Skill @ DN %d", iCost + 5);
		
		return super.propagateChange();
	}	
}

class SFXFractionUnreal extends SFXFraction
{
	private static final long serialVersionUID = -3191355227493376437L;

	private int m_iDisbelief = 0;
	private SFXBase m_fieldEffect = null;
	
	@Override
	public int getCost()
	{
		int iCost = ((m_fieldEffect.getCost() * m_iDisbelief) + 99) / 100;
		return iCost;
	}
	
	@Override
	public SFXField setValue(String scValue)
	{
		if (scValue.startsWith("LEVEL"))
		{
			// For radio buttons sets level 0%, 25%, 50%, 75%
			int iValue = Integer.parseInt(scValue.substring(6));
			return setDisbelief(iValue);
		}
		
		// Otherwise set proportion
		return super.setValue(scValue);
	}
	
	public int getDisbelief()
	{
		return m_iDisbelief;
	}
	
	private SFXField setDisbelief(int iDisbelief)
	{
		m_iDisbelief = iDisbelief;
		return propagateChange();
	}
	
	@Override
	public SFXBase addDepends(SFXBase fieldEffect)
	{
		m_fieldEffect = fieldEffect;
		return super.addDepends(fieldEffect);
	}
}

class SFXFieldModifiers extends SFXDouble
{
	private static final long serialVersionUID = 6843393368644151314L;

	// Set when the values for reduction great than allowed
	private boolean m_zLimited = false;
	private SFXCompositeActionReduction m_actionReduction = null;
	
	@Override
	public SFXBase createField(String scField)
	{
		m_actionReduction = new SFXCompositeActionReduction();
		return super.createField(scField);
	}
	
	@Override
	public SFXField updateValue()
	{
		int iIndex = 0;
		int iMandatoryCost = m_listSource.get(iIndex++).getCost();
		int iLimitDN = iMandatoryCost;
		iLimitDN += m_listSource.get(iIndex++).getCost();

		// Fraction split to limits
		SFXFraction fraction = (SFXFraction) m_listSource.get(iIndex++);
		int iLimitFV = iLimitDN - fraction.getCost(iLimitDN);
		iLimitDN = iLimitDN - iLimitFV;
				
		SFXComposite compositeCommunity = (SFXComposite)m_listSource.get(iIndex++);
		int iReduceDN = compositeCommunity.getCost();
		int iReduceFV = m_actionReduction.getReduction(compositeCommunity);
				
		SFXComposite compositeComponents = (SFXComposite)m_listSource.get(iIndex++);
		iReduceDN += compositeComponents.getCost();
		iReduceFV += m_actionReduction.getReduction(compositeComponents);
		
		SFXValueModifier valueConcentration = (SFXValueModifier)m_listSource.get(iIndex++);
		iReduceDN += valueConcentration.getCost();
		iReduceFV += valueConcentration.getReduction();

		SFXComposite compositeGestures = (SFXComposite)m_listSource.get(iIndex++);
		iReduceDN += compositeGestures.getCost();
		iReduceFV += m_actionReduction.getReduction(compositeGestures);
		
		SFXValueModifier valueIncantation = (SFXValueModifier)m_listSource.get(iIndex++);
		iReduceDN += valueIncantation.getCost();
		iReduceFV += valueIncantation.getReduction();

		SFXValueModifier valueRelatedSkill = (SFXValueModifier)m_listSource.get(iIndex++);
		iReduceDN += valueRelatedSkill.getCost();
		iReduceFV += valueRelatedSkill.getReduction();
		
		SFXDouble doubleOther = (SFXDouble)m_listSource.get(iIndex++);
		iReduceDN += doubleOther.getValueDN();
		iReduceFV += doubleOther.getValueFV();
		
		SFXFraction fractionUnreal = (SFXFraction)m_listSource.get(iIndex++);
		int iTotal = fractionUnreal.getCost();
		iReduceDN += fractionUnreal.getCost(iTotal);
		iReduceFV += iTotal - fractionUnreal.getCost(iTotal);
		
		iLimitDN = iLimitDN / 2;
		iLimitFV = iLimitFV / 2;
		
		m_zLimited = false;
		if (iReduceDN>iLimitDN) m_zLimited = true;
		if (iReduceFV>iLimitFV) m_zLimited = true;
		
		if ((iReduceDN + iReduceFV)>iMandatoryCost) m_zLimited = true;
		
		m_iValueDN = iReduceDN;
		m_iValueFV = iReduceFV;
		
		return propagateChange();
	}
	
	public boolean isLimited()
	{
		return m_zLimited;
	}	
}			

class SFXFieldTotal extends SFXDouble
{
	private static final long serialVersionUID = -2927790560895105714L;

	private SFXCalculated m_calculatedMandatory = null;
	private SFXCalculated m_calculatedOptional = null;
	private SFXFraction m_fraction = null;
	private SFXDouble m_doubleModifiers = null;
	
	@Override
	public SFXField updateValue()
	{
		int iMandatory = m_calculatedMandatory.getCost();
		int iOptional = m_calculatedOptional.getCost();
		
		int iCostDN = m_fraction.getCost(iMandatory+iOptional);
		int iCostFV = iMandatory + iOptional - iCostDN;
		
		int iReduceDN = m_doubleModifiers.getValueDN();
		int iReduceFV = m_doubleModifiers.getValueFV();
		
		m_iValueDN = iCostDN - iReduceDN;
		m_iValueDN = m_iValueDN>0?m_iValueDN:0;
		
		m_iValueFV = iCostFV - iReduceFV;
		m_iValueFV = m_iValueFV>0?m_iValueFV:0;
		
		return propagateChange();
	}
	
	public SFXFieldTotal setSources(SFXCalculated calculatedMandatory, SFXCalculated calculatedOptional, SFXFraction fraction, SFXDouble doubleModifiers)
	{
		m_calculatedMandatory = calculatedMandatory;
		m_calculatedOptional = calculatedOptional;
		m_fraction = fraction;
		m_doubleModifiers = doubleModifiers;
		return this;
	}
}

class SFXFieldConcentrationAdd extends SFXCalculated
{
	private static final long serialVersionUID = 3704525773749452228L;
	private SFXFieldConcentration m_fieldConcentration = null;
	
	@Override
	public int getCost()
	{
		if (0==m_iValue) return 0;
		if (getConcentration()) return 2;
		return 0;
	}

	public SFXFieldConcentrationAdd setConcentration(SFXBase fieldConcentration) 
	{
		m_fieldConcentration = (SFXFieldConcentration)fieldConcentration;
		return this;
	}
	
	public boolean getConcentration()
	{
		int iConcentration = m_fieldConcentration.getCost() + m_fieldConcentration.getReduction();
		return 0<iConcentration;
	}
}

class SFXFieldReception extends SFXValue
{
	private static final long serialVersionUID = 3282111078226664451L;

	private boolean m_zIdentification = false;
	private SFXBase m_fieldSkill = null;
	
	public SFXFieldReception setSkill(SFXBase fieldSkill)
	{
		m_fieldSkill = fieldSkill;
		return this;
	}
	
	@Override
	public int getCost()
	{
		int iCost = m_Multiplier.getOffset();
		if (0==iCost) return 0;
		
		if (m_zIdentification) iCost += 1;
		
		int iSkill = m_fieldSkill.getCost();
		if (SFXSkill._SHAMAN==iSkill) iCost -= 1;
		
		return iCost;
	}
	
	@Override
	public int getBonus()
	{
		if (m_zIdentification) return 1;
		return 0;
	}
}

class SFXFieldTrance extends SFXValue
{
	private static final long serialVersionUID = -5139274464946043526L;

	private static final int _DEFINED_NECROSCOPE = 5;
	
	private SFXBase m_fieldSkill = null;
	
	public SFXFieldTrance setSkill(SFXBase fieldSkill)
	{
		m_fieldSkill = fieldSkill;
		return this;
	}
	
	@Override
	public SFXField updateValue()
	{
		int iSkill = m_fieldSkill.getCost();
		if (SFXSkill._NECROSCOPE!=iSkill) return changedField();
		
		int iCost = m_Multiplier.getOffset();
		if (0==iCost) m_Multiplier.setIndex(_DEFINED_NECROSCOPE);

		return propagateChange();
	}
	
	@Override
	public int getCost()
	{
		int iCost = m_Multiplier.getOffset();
		int iSkill = m_fieldSkill.getCost();
		if (SFXSkill._NECROSCOPE==iSkill) iCost -= _DEFINED_NECROSCOPE;
		
		return iCost;
	}	
}

class SFXFieldTotalN extends SFXDouble
{
	private static final long serialVersionUID = -5499003058583842557L;

	private List<SFXBase> m_listSource = null;
	
	@Override
	public SFXBase createField(String scField)
	{
		m_listSource = new CopyOnWriteArrayList<SFXBase>();
		return super.createField(scField);
	}
	
	@Override
	public SFXField updateValue()
	{
		int iIndex = 0;
		
		SFXCalculated fieldMandatory = (SFXCalculated)m_listSource.get(iIndex++);
		int iMandatory = fieldMandatory.getCost();
		SFXCalculated fieldOptional = (SFXCalculated)m_listSource.get(iIndex++);
		int iOptional = fieldOptional.getCost();
		
		SFXFraction fieldFraction = (SFXFraction)m_listSource.get(iIndex++);
		
		SFXDouble doubleModifiers = (SFXDouble)m_listSource.get(iIndex++);
		
		// Value of lock is not a reduction
		SFXValue valueLock = (SFXValue)m_listSource.get(iIndex++);
		int iCost = -valueLock.getCost();
		
		// All other things are a reduction in cost
		for (int iSize = m_listSource.size(); iIndex<iSize; ++iIndex)
		{
			SFXBase field = m_listSource.get(iIndex);
			iCost += field.getCost();
		}
		
		m_iValue = iCost;
		
		int iCostDN = fieldFraction.getCost(iMandatory+iOptional-iCost);
		int iCostFV = iMandatory + iOptional - iCost - iCostDN;
		
		int iReduceDN = doubleModifiers.getValueDN();
		int iReduceFV = doubleModifiers.getValueFV();
		
		m_iValueDN = iCostDN - iReduceDN;
		m_iValueDN = m_iValueDN>0?m_iValueDN:0;
		
		m_iValueFV = iCostFV - iReduceFV;
		m_iValueFV = m_iValueFV>0?m_iValueFV:0;
		
		return propagateChange();
	}
	
	@Override
	public SFXBase addDepends(SFXBase fieldDepends)
	{
		// Add source field to list held so have list of sources to recalculate value
		m_listSource.add(fieldDepends);
		return super.addDepends(fieldDepends);
	}
}
