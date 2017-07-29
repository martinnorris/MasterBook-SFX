
package sfx;

import java.util.List;

public interface SFXText
{
	public boolean hasUpdate();
	public boolean includeContent();
	public boolean useValue();
	public String getValue();
}

class SFXTextBase implements SFXText
{
	protected SFXBase m_fieldContent = null;
	private boolean m_zCheck = false;
	
	protected SFXTextBase m_includeContent = null;
	protected SFXTextBase m_getValue = null;
	
	private String m_scValue = null;
	
	public SFXTextBase setField(SFXBase fieldContent) 
	{
		m_fieldContent = fieldContent;
		if (null!=m_includeContent) m_includeContent.setField(fieldContent);
		if (null!=m_getValue) m_getValue.setField(fieldContent);
		return this;
	}
	
	public SFXTextBase setAccess(SFXViewQuery queryView, String scAccess) 
	{
		/* ==================================================================
		 * Section for include checks that are calculation on values
		 * ================================================================== */
		
		if (scAccess.contains(".eq."))
		{
			// Strip off the divide and get the source iteratively
			String[] ascContent = scAccess.split("[.]eq[.]", 2);
			// Link the source in a chain with calculation on top
			SFXTextEqual textValue = new SFXTextEqual();
			m_includeContent = textValue.setCalculation(ascContent[1]).setAccess(queryView, ascContent[0]).setField(m_fieldContent).setCheck(m_zCheck);
			return this;
		}
		
		if (scAccess.contains(".neq."))
		{
			// Strip off the divide and get the source iteratively
			String[] ascContent = scAccess.split("[.]neq[.]", 2);
			// Link the source in a chain with calculation on top
			SFXTextNotEqual textValue = new SFXTextNotEqual();
			m_includeContent = textValue.setCalculation(ascContent[1]).setAccess(queryView, ascContent[0]).setField(m_fieldContent).setCheck(m_zCheck);
			return this;
		}
		
		if (scAccess.contains(".gt."))
		{
			// Look for ^^ for 'more than' because don't want to use > in the HTML!
			String[] ascContent = scAccess.split("[.]gt[.]", 2);
			// Link the source in a chain with calculation on top
			SFXTextMoreThan textValue = new SFXTextMoreThan();
			m_includeContent = textValue.setCalculation(ascContent[1]).setAccess(queryView, ascContent[0]).setField(m_fieldContent).setCheck(m_zCheck);
			return this;
		}
		
		if (scAccess.contains(".lt."))
		{
			// Look for ^^ for 'more than' because don't want to use > in the HTML!
			String[] ascContent = scAccess.split("[.]lt[.]", 2);
			// Link the source in a chain with calculation on top
			SFXTextLessThan textValue = new SFXTextLessThan();
			m_includeContent = textValue.setCalculation(ascContent[1]).setAccess(queryView, ascContent[0]).setField(m_fieldContent).setCheck(m_zCheck);
			return this;
		}
		
		if (scAccess.startsWith("not."))
		{
			// Link the source in a chain with calculation on top
			SFXTextNot textValue = new SFXTextNot();
			m_includeContent = textValue.setAccess(queryView, scAccess.substring(4)).setField(m_fieldContent).setCheck(m_zCheck);
			return this;
		}
		
		/* ==================================================================
		 * Section for explicit include checks
		 * ================================================================== */
		
		if (scAccess.startsWith("hasCost"))
		{
			// hasCost useful for checking whether section should be added
			m_includeContent = new SFXTextCost();
			m_includeContent.setField(m_fieldContent);
			return this;
		}
		
		if (scAccess.startsWith("isDefault"))
		{
			m_includeContent = new SFXTextValueDefault();
			m_includeContent.setField(m_fieldContent);
			return this;
		}
		
		/* ==================================================================
		 * Second section for calculation on values found
		 * ================================================================== */
		
		if (scAccess.contains(".div."))
		{
			// Strip off the divide and get the source iteratively
			String[] ascContent = scAccess.split("[.]div[.]", 2);
			// Link the source in a chain with calculation on top
			SFXTextDivide textValue = new SFXTextDivide();
			m_getValue = textValue.setCalculation(ascContent[1]).setAccess(queryView, ascContent[0]).setField(m_fieldContent).setCheck(m_zCheck);
			return this;
		}
		
		if (scAccess.contains(".mul."))
		{
			// Strip off the divide and get the source iteratively
			String[] ascContent = scAccess.split("[.]mul[.]", 2);
			// Link the source in a chain with calculation on top
			SFXTextMultiply textValue = new SFXTextMultiply();
			m_getValue = textValue.setCalculation(ascContent[1]).setAccess(queryView, ascContent[0]).setField(m_fieldContent).setCheck(m_zCheck);
			return this;
		}
		
		if (scAccess.contains(".show."))
		{
			// Strip off the divide and get the source iteratively
			String[] ascContent = scAccess.split("[.]show[.]", 2);
			// Link the source in a chain with calculation on top
			SFXTextBoolean textValue = new SFXTextBoolean();
			m_getValue = textValue.setCalculation(ascContent[1]).setAccess(queryView, ascContent[0]).setField(m_fieldContent).setCheck(m_zCheck);
			return this;
		}
		
		if (scAccess.contains(".bit."))
		{
			// Strip off the divide and get the source iteratively
			String[] ascContent = scAccess.split("[.]bit[.]", 2);
			// Link the source in a chain with calculation on top
			SFXTextBit textValue = new SFXTextBit();
			m_getValue = textValue.setCalculation(ascContent[1]).setAccess(queryView, ascContent[0]).setField(m_fieldContent).setCheck(m_zCheck);
			return this;
		}
		
		/* ==================================================================
		 * Section for getting values
		 * ================================================================== */
		
		if (scAccess.startsWith("getIndex"))
		{
			m_getValue = new SFXTextIndex();
			m_getValue.setField(m_fieldContent);
			return this;
		}
		
		if (scAccess.startsWith("getValueUnit"))
		{
			// Before getValue so takes priority
			m_getValue = new SFXTextValueUnit();
			m_getValue.setField(m_fieldContent);
			return this;
		}
		
		if (scAccess.startsWith("getValue"))
		{
			String[] ascContent = scAccess.split("getValue[.]", 2);
			SFXTextValue textValue = new SFXTextValue();
			if (2==ascContent.length) 
				m_getValue = textValue.setAccess(queryView, ascContent[1]);
			else
				m_getValue = textValue;
			m_getValue.setField(m_fieldContent).setCheck(m_zCheck);
			return this;
		}
		
		if (scAccess.startsWith("getChildren"))
		{
			m_getValue = new SFXTextComposite();
			m_getValue.setField(m_fieldContent);
			return this;
		}
		
		if (scAccess.startsWith("getCostReduction"))
		{
			m_getValue = new SFXTextValueModifier();
			m_getValue.setField(m_fieldContent);
			return this;
		}
		
		if (scAccess.startsWith("getCostDN"))
		{
			m_getValue = new SFXTextDoubleDN();
			m_getValue.setField(m_fieldContent);
			return this;
		}
		
		if (scAccess.startsWith("getCostFV"))
		{
			String[] ascContent = scAccess.split("getCostFV[.]", 2);
			SFXTextDoubleFV textValue = new SFXTextDoubleFV();
			if (2==ascContent.length) 
				m_getValue = textValue.setAccess(queryView, ascContent[1]);
			else
				m_getValue = textValue;
			m_getValue.setField(m_fieldContent).setCheck(m_zCheck);
			return this;
		}
		
		if (scAccess.startsWith("getCost"))
		{
			m_getValue = new SFXTextCost();
			m_getValue.setField(m_fieldContent);
			return this;
		}
		
		if (scAccess.startsWith("getTable"))
		{
			m_getValue = new SFXTextValueTable();
			m_getValue.setField(m_fieldContent);
			return this;
		}
		
		if (scAccess.startsWith("getUnit"))
		{
			m_getValue = new SFXTextUnit();
			m_getValue.setField(m_fieldContent);
			return this;
		}
		
		if (scAccess.startsWith("getComment"))
		{
			m_getValue = new SFXTextComment();
			m_getValue.setField(m_fieldContent);
			return this;
		}
		
		if (scAccess.startsWith("getTime"))
		{
			// Want to apply the table to the value on the LHS of the expression
			SFXTextTable tableTime = new SFXTextTable();
			tableTime.setTable(SFXTable.getDefaultValueTable(), new SFXTime());
			tableTime.m_getValue = this;
			return tableTime;
		}
		
		if (scAccess.startsWith("getMove"))
		{
			// Before getValue so takes priority
			SFXTextMove valueMove = new SFXTextMove();
			m_getValue = valueMove.setViewQuery(queryView).setField(m_fieldContent);
			return this;
		}
		
		if (scAccess.startsWith("getPercentage"))
		{
			m_getValue = new SFXTextFraction();
			m_getValue.setField(m_fieldContent);
			return this;
		}		
		
		if (scAccess.startsWith("getBonus"))
		{
			m_getValue = new SFXTextBonus();
			m_getValue.setField(m_fieldContent);
			return this;
		}		
		
		if (scAccess.startsWith("getSpecial"))
		{
			m_getValue = new SFXTextSpecial();
			m_getValue.setField(m_fieldContent);
			return this;
		}
		
		if (scAccess.startsWith("getOpposition"))
		{
			m_getValue = new SFXTextSkillOpposition();
			m_getValue.setField(m_fieldContent);
			return this;
		}
			
		if (scAccess.startsWith("getResult"))
		{
			m_getValue = new SFXTextSkillResult();
			m_getValue.setField(m_fieldContent);
			return this;
		}
		
		throw new RuntimeException("Could not map " + scAccess);
	}
	
	public SFXTextBase setCheck(boolean zIsHeader) 
	{
		m_zCheck = zIsHeader;
		return this;
	}
	
	@Override
	public boolean hasUpdate() 
	{
		// Minimise updates by only responding to data that has really changed
		String scValue = m_fieldContent.getValue();
		if (scValue==m_scValue) return false;
		m_scValue = scValue;
		return true;
	}
	
	@Override
	public boolean includeContent()
	{
		// Check field should be included {depends on type}
		if (m_zCheck) return m_includeContent.includeContent();
		// Otherwise not important what the value is
		return true;
	}
	
	@Override
	public boolean useValue()
	{
		return !m_zCheck;
	}
	
	@Override
	public String getValue()
	{
		return m_getValue.getValue();
	}
	
	@Override
	public String toString()
	{
		if (null!=m_includeContent) return m_includeContent.toString();
		if (null!=m_getValue) return m_getValue.toString();
		return "<unset>";
	}
}

class SFXTextLink extends SFXTextBase
{
	private SFXTextBase m_linkField = null;
	
	@Override
	public SFXTextBase setField(SFXBase fieldContent) 
	{
		m_linkField.setField(fieldContent);
		return super.setField(fieldContent);
	}
	
	public SFXTextLink linkField(SFXTextBase linkWrapper)
	{
		m_linkField = linkWrapper;
		return this;
	}
}

class SFXTextCalculation extends SFXTextBase
{
	protected int m_iValue = 0;
	protected double m_dOperand = 0.0;
	protected boolean m_zRoundUp = false;
	
	public SFXTextCalculation setCalculation(String scCalculation)
	{
		// Strip any number followed by U
		String[] ascContent = scCalculation.split("(?<![0-9.]+)(?=[0-9.]+[U]?)"); // Use '[0-9.]' instead of '//d' to match a fp number
		// If there are any calculations after the number then add to this field
		if (1<ascContent.length) setAccess(null, ascContent[1]);
		if (ascContent[0].endsWith("U")) return setRoundUp(ascContent[0].substring(0, ascContent[0].length()-1));
		m_dOperand = Double.parseDouble(scCalculation);
		m_iValue = (int)(m_zRoundUp?Math.ceil(m_dOperand):Math.floor(m_dOperand));
		return this;
	}
	
	private SFXTextCalculation setRoundUp(String scCalculation)
	{
		m_zRoundUp = true;
		return setCalculation(scCalculation);
	}
}

class SFXTextEqual extends SFXTextCalculation
{
	// Key is '<something>.eq.<number>' for checking inclusion
	
	@Override
	public boolean includeContent()
	{
		String scValue = m_getValue.getValue();
		float fValue = Float.parseFloat(scValue);
		int iValue = (int)fValue;
		return iValue==m_iValue;
	}
	
	@Override
	public String toString()
	{
		return String.format("%s .eq. %d ", m_getValue.toString(), m_iValue);
	}
}

class SFXTextNotEqual extends SFXTextCalculation
{
	// Key is '<something>.neq.<number>' for checking inclusion
	
	@Override
	public boolean includeContent()
	{
		String scValue = m_getValue.getValue();
		//int iValue = Integer.parseInt(scValue);
		//return iValue!=m_iValue;
		double dValue = Double.parseDouble(scValue);
		return dValue!=this.m_dOperand;
	}

	@Override
	public String toString()
	{
		return String.format("%s .neq. %d ", m_getValue.toString(), m_iValue);
	}
}

class SFXTextMoreThan extends SFXTextCalculation
{
	// Key is '<something>.gt.<number>' for checking inclusion
	
	@Override
	public boolean includeContent()
	{
		String scValue = m_getValue.getValue();
		int iValue = Integer.parseInt(scValue);
		return iValue>m_iValue;
	}
	
	@Override
	public String toString()
	{
		return String.format("%s .gt. %d ", m_getValue.toString(), m_iValue);
	}
}

class SFXTextLessThan extends SFXTextCalculation
{
	// Key is '<something>.lt.<number>' for checking inclusion
	
	@Override
	public boolean includeContent()
	{
		String scValue = m_getValue.getValue();
		int iValue = Integer.parseInt(scValue);
		return iValue<m_iValue;
	}
	
	@Override
	public String toString()
	{
		return String.format("%s .lt. %d ", m_getValue.toString(), m_iValue);
	}
}

class SFXTextBoolean extends SFXTextCalculation
{
	protected String m_scTrue = null;
	
	// Key is '<something>.show.<text>' for getting value and if set returning text
	
	@Override
	public SFXTextCalculation setCalculation(String scCalculation)
	{
		m_scTrue = scCalculation;
		return this;
	}
	
	@Override
	public String getValue()
	{
		String scValue = m_getValue.getValue();
		int iValue = Integer.parseInt(scValue);
		if (0==iValue) return "";
		return m_scTrue;
	}
	
	@Override
	public String toString()
	{
		return String.format("%s .show. '%s' ", m_getValue.toString(), m_scTrue);
	}
}

class SFXTextDivide extends SFXTextCalculation
{
	// Key is '<something>.div.<number>[|U|D]' for getting value
	
	@Override
	public String getValue()
	{
		String scValue = m_getValue.getValue();
		double dValue = Double.parseDouble(scValue);
		dValue /= m_dOperand;
		if (!m_zRoundUp) return Double.toString(dValue);
		int iValue = (int) Math.ceil(dValue);	
		return Integer.toString(iValue);
	}
	
	@Override
	public String toString()
	{
		return String.format("%s .div. %f %s ", m_getValue.toString(), m_dOperand, m_zRoundUp?"(round up)":"");
	}
}

class SFXTextMultiply extends SFXTextCalculation
{
	// Key is '<something>.mul.<number>' for getting value
	
	@Override
	public String getValue()
	{
		String scValue = m_getValue.getValue();
		double dValue = Double.parseDouble(scValue);
		dValue *= m_dOperand;
		if (!m_zRoundUp) return Double.toString(dValue);
		int iValue = (int) Math.ceil(dValue);	
		return Integer.toString(iValue);
	}
	
	@Override
	public String toString()
	{
		return String.format("%s .mul. %f %s ", m_getValue.toString(), m_dOperand, m_zRoundUp?"(round up)":"");
	}
}

class SFXTextBit extends SFXTextCalculation
{
	// Key is '<something>.bit.<number>' for getting value of a particular bit
	
	@Override
	public String getValue()
	{
		String scValue = m_getValue.getValue();
		int iValue = Integer.parseInt(scValue);
		iValue &= m_iValue;
		return Integer.toString(iValue);
	}
	
	@Override
	public String toString()
	{
		return String.format("%s .bit. %d ", m_getValue.toString(), m_iValue);
	}
}

class SFXTextNot extends SFXTextCalculation
{
	// Key is '<field>.not.<something>' for checking 'not something'
	
	@Override
	public boolean includeContent()
	{
		return !m_includeContent.includeContent();
	}
	
	@Override
	public String toString()
	{
		return String.format("not.%s", m_includeContent.toString());
	}
}

class SFXTextCost extends SFXTextBase
{
	// Key is 'hasCost' for getting value
	
	@Override
	public boolean includeContent()
	{
		// Include content when non zero cost
		int iCost = m_fieldContent.getCost();
		return 0<iCost;
	}
	
	// Key is 'getCost' for getting value
	
	@Override
	public String getValue()
	{
		return Integer.toString(m_fieldContent.getCost());
	}
	
	@Override
	public String toString()
	{
		return "getCost()";
	}
}

class SFXTextIndex extends SFXTextBase
{
	// Key is 'getIndex' for getting value
	
	@Override
	public String getValue()
	{
		SFXScalar scalar = (SFXScalar) m_fieldContent;
		return Integer.toString(scalar.getIndex());
	}
	
	@Override
	public String toString()
	{
		return "getIndex()";
	}
}

class SFXTextValue extends SFXTextCost
{
	// Key is 'getValue' for getting the value
	
	@Override
	public String getValue()
	{
		return m_fieldContent.getValue();
	}		
	
	@Override
	public String toString()
	{
		return "getValue()";
	}
}

class SFXTextComposite extends SFXTextValue
{
	// Key is 'getChildren' for getting the value
	
	@Override
	public String getValue()
	{
		SFXComposite composite = (SFXComposite) m_fieldContent;
		List<SFXBase> listFields = composite.getComposition();
		
		// count the number of active children
		int iSet = 0;
		for (SFXBase field : listFields)
		{
			if (field instanceof SFXValueModifier)
			{
				SFXValueModifier value = (SFXValueModifier) field;
				int iTotal = value.getCost() + value.getReduction();
				if (0<iTotal) iSet += 1;
				continue;
			}
			
			if (field instanceof SFXDouble)
			{
				SFXDouble value = (SFXDouble) field;
				int iTotal = value.getValueDN() + value.getValueFV();
				if (0<iTotal) iSet += 1;
				continue;
			}
			
			if (0<field.getCost()) iSet += 1;
		}
		
		return Integer.toString(iSet);
	}	
	
	@Override
	public String toString()
	{
		return "getChildren()";
	}
}

class SFXTextValueTable extends SFXTextValue
{
	// Key is 'getTable' for getting the value
	
	@Override
	public String getValue()
	{
		// This is for the case where the table has special values and getValue gives the index for these instead of a value from the table
		SFXValue valueField = (SFXValue) m_fieldContent;
		String scValue = valueField.getValue();
		int iValue = Integer.parseInt(scValue);
		String[] ascValues = valueField.getTableContent();
		return ascValues[iValue];
	}	
	
	@Override
	public String toString()
	{
		return "getTable()";
	}
}

class SFXTextValueUnit extends SFXTextCost
{
	// Key is 'getValueUnit' for getting the SFXValue value and units (e.g. '15 seconds')
	
	@Override
	public String getValue()
	{
		SFXValue valueField = (SFXValue) m_fieldContent;
		
		StringBuilder sb = new StringBuilder();
		sb.append(m_fieldContent.getValue());
		sb.append(' ');
		sb.append(valueField.getUnit());
		
		return sb.toString();
	}	
	
	@Override
	public String toString()
	{
		return "getValueUnit()";
	}
}

class SFXTextValueModifier extends SFXTextCost
{
	// Key is 'getCostReduction' for getting the SFXDouble cost units, DN and FV together (e.g. DN + FV)
	
	@Override
	public String getValue()
	{
		SFXValueModifier valueField = (SFXValueModifier) m_fieldContent;
		int iCost = valueField.getCost(); 
		int iReduction = valueField.getReduction();
		return Integer.toString(iCost + iReduction);
	}		
	
	@Override
	public String toString()
	{
		return "getCostReduction()";
	}
}

class SFXTextComment extends SFXTextCost
{
	// Key is 'getComment' for getting the SFXDouble cost units
	
	@Override
	public String getValue()
	{
		SFXValueModifier valueField = (SFXValueModifier) m_fieldContent;
		return valueField.getComment();
	}		

	@Override
	public String toString()
	{
		return "getComment()";
	}
}

class SFXTextUnit extends SFXTextValueUnit
{
	// Key is 'getUnit' for getting the SFXValue units
	
	@Override
	public String getValue()
	{
		SFXValue valueField = (SFXValue) m_fieldContent;
		return valueField.getUnit();
	}
	
	@Override
	public String toString()
	{
		return "getUnit()";
	}
}

class SFXTextDoubleDN extends SFXTextCost
{
	// Key is 'getCostDN' for getting the SFXDouble cost units
	
	@Override
	public String getValue()
	{
		SFXDouble valueField = (SFXDouble) m_fieldContent;
		int iCostDN = valueField.getValueDN(); 
		return Integer.toString(iCostDN);
	}
	
	@Override
	public String toString()
	{
		return "getCostDN()";
	}
}

class SFXTextDoubleFV extends SFXTextCost
{
	// Key is 'getCostDN' for getting the SFXDouble cost units
	
	@Override
	public String getValue()
	{
		SFXDouble valueField = (SFXDouble) m_fieldContent;
		int iCostDN = valueField.getValueFV(); 
		return Integer.toString(iCostDN);
	}
	
	@Override
	public String toString()
	{
		return "getCostFV()";
	}	
}

class SFXTextValueDefault extends SFXTextValue
{
	// Key is 'isDefault' for checking includeContent
	
	@Override
	public boolean includeContent()
	{
		SFXValueFollows valueField = (SFXValueFollows) m_fieldContent;
		return valueField.getDefault();
	}
	
	@Override
	public String toString()
	{
		return "isDefault()";
	}
}

class SFXTextTable extends SFXTextBase
{
	private SFXTable m_Table = null;
	private SFXTableMultiplier m_Multiplier = null;
	
	@Override
	public String getValue()
	{
		String scValue = m_getValue.getValue();
		int iValue = Integer.parseInt(scValue);
		
		String[] ascContent = m_Multiplier.getContent();
		
		// First find the best multiplier to set units
		int iCount = 0;
		
		for (iCount = 1; iCount<ascContent.length; ++iCount)
		{
			int iOffset = m_Multiplier.setIndex(iCount).getOffset();
			if (iOffset>iValue) break;
		}
		
		iValue -= m_Multiplier.setIndex(iCount-1).getOffset();
		
		// Cross reference remaining cost on the table
		float fValue = m_Table.getValue(iValue);	
		
		return String.format("%.1f %s", fValue, ascContent[iCount-1]);
	}
	
	public SFXTextTable setTable(SFXTable table, SFXTableMultiplier multiplier)
	{
		m_Table = table;
		m_Multiplier = multiplier;
		return this;
	}
	
	@Override
	public String toString()
	{
		return m_getValue + ".getTime()";
	}
}

class SFXTextMove extends SFXTextValueUnit
{
	private SFXViewQuery m_queryView = null;
	private int m_iOffsetMove = 0;
	
	// Key is 'getMove' for getting the SFXValue value units for move which requires peeking at weight units chosen (e.g. builds move text)
	
	@Override
	public String getValue()
	{
		SFXValueMove valueMove = (SFXValueMove)m_fieldContent;
		
		float fTry = valueMove.getMass();
		int iOffsetMass = valueMove.getMultiplierIndex();
		int iDeltaEffectMassCost = valueMove.getDeltaEffectMassCost(fTry, iOffsetMass);
		
		m_iOffsetMove = getMoveOffset();
		float fMove = valueMove.getMoveCost(iDeltaEffectMassCost, m_iOffsetMove);
		
		StringBuilder sb = new StringBuilder();
		
		if (0>fMove)
			sb.append("Weight cannot be moved");
		else
			sb.append(Float.toString(fMove));	
		
		sb.append(' ');

		String[] ascUnits = valueMove.getMultiplierContentEx();
		sb.append(ascUnits[m_iOffsetMove]);
		
		return sb.toString();
	}
	
	public SFXTextMove setViewQuery(SFXViewQuery queryView)
	{
		m_queryView = queryView;
		return this;
	}
	
	private int getMoveOffset()
	{
		// The field could change on the view so always query for the offset
		SFXViewField view = m_queryView.getView(m_fieldContent);
		// If offset not found just return the previous value
		if (null==view) return m_iOffsetMove;
		SFXSwingApportationMove viewMove = (SFXSwingApportationMove)view;
		return viewMove.getMoveOffset();
	}

	@Override
	public String toString()
	{
		return "getMove()";
	}
}

class SFXTextFraction extends SFXTextCost
{
	// Key is 'getPercentage' for getting destroyed boolean
	
	@Override
	public String getValue()
	{
		SFXFraction fraction = (SFXFraction)m_fieldContent;
		int iDN = fraction.getCost(100);
		int iFV = 100 - fraction.getCost(100);
		return String.format("%d%%/%d%%", iDN, iFV);
	}
	
	@Override
	public String toString()
	{
		return "getPercentage()";
	}
}

class SFXTextBonus extends SFXTextCost
{
	// Key is 'getBonus' for getting bonus made up of several checks
	
	@Override
	public String getValue()
	{
		SFXScalar scalar = (SFXScalar)m_fieldContent;
		int iValue = scalar.getBonus();
		return Integer.toString(iValue);
	}
	
	@Override
	public String toString()
	{
		return "getBonus()";
	}
}

class SFXTextSpecial extends SFXTextCost
{
	// Key is 'getBonus' for getting bonus made up of several checks
	
	@Override
	public String getValue()
	{
		if (m_fieldContent instanceof SFXFractionUnreal) return getSpecialUnreal();
		return m_fieldContent.toString();
	}
	
	private String getSpecialUnreal()
	{
		SFXFractionUnreal fractionUnreal = (SFXFractionUnreal) m_fieldContent;
		int iFraction = fractionUnreal.getDisbelief();
		if (75<=iFraction) return "0";
		if (50<=iFraction) return "6";
		if (25<=iFraction) return "10";
		return "REAL";
	}
	
	@Override
	public String toString()
	{
		return "getSpecial()";
	}
}

class SFXTextSkill extends SFXTextCost
{
	protected static SFXSkill m_tableSkill = null;
	
	@Override
	public String getValue()
	{
		if (null==m_tableSkill) m_tableSkill = new SFXSkill();
		SFXValue valueSkill = (SFXValue)m_fieldContent;
		return getBasis(valueSkill.getMultiplierIndex());
	}
	
	protected String getBasis(int iValue)
	{
		return m_tableSkill.getBasis(iValue)[SFXSkill._SKILL];
	}
	
	@Override
	public String toString()
	{
		return "getSkill()";
	}
}

class SFXTextSkillResult extends SFXTextSkill
{
	// Key is 'getResult' for getting result used for effect
	
	@Override
	protected String getBasis(int iValue)
	{
		return m_tableSkill.getBasis(iValue)[SFXSkill._RESULT];
	}
	
	@Override
	public String toString()
	{
		return "getResult()";
	}
}
class SFXTextSkillOpposition extends SFXTextSkill
{
	// Key is 'getOpposition' for getting opposing attribute

	@Override
	protected String getBasis(int iValue)
	{
		return m_tableSkill.getBasis(iValue)[SFXSkill._OPPOSITION];
	}
	
	@Override
	public String toString()
	{
		return "getOpposition()";
	}
}
