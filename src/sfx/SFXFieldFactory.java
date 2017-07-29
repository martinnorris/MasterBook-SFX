package sfx;

import java.util.Map;

public class SFXFieldFactory extends SFXBase
{
	private static final long serialVersionUID = 3878756449108400807L;

	private Map<String, SFXBase> m_mapFields = null;

	@Override
	protected SFXBase getInstance()
	{
		SFXBase field = new SFXBase();
		field.addListeners(m_listFieldListeners).createField(m_scField);
		return replaceParent(field);
	}		
	
	protected SFXBase setFields(Map<String, SFXBase> mapFields)
	{
		m_mapFields = mapFields;
		return this;
	}
	
	protected SFXBase getField(String scField)
	{
		return m_mapFields.get(scField).getInstance();
	}
	
	protected SFXBase replaceParent(SFXBase fieldCreated)
	{
		m_mapFields.replace(m_scField, fieldCreated);
		return fieldCreated;
	}
}

/* ==========================================================================
   Specializations of the different fields for the different areas of the form
   ========================================================================== */

class SFXFieldFactoryEffect extends SFXFieldFactory
{
	private static final long serialVersionUID = 3936962389140497076L;

	@Override
	protected SFXBase getInstance()
	{
		SFXScalar scalarEffect = new SFXScalar();
		scalarEffect.createField("Effect").addListeners(m_listFieldListeners);
		return replaceParent(scalarEffect);
	}	
}

class SFXFieldFactoryRange extends SFXFieldFactory
{
	private static final long serialVersionUID = 5332139888658893107L;

	@Override
	protected SFXBase getInstance()
	{
		SFXValue valueRange = new SFXValue();
		valueRange.createField("Range").addListeners(m_listFieldListeners);
		// Set the table entry {for the value 0}, the type and the value
		valueRange.setTable(SFXTable.getDefaultValueTable()).setMultiplierTable(new SFXDistance()).setValue(0);
		return replaceParent(valueRange);
	}	
}

class SFXFieldFactorySpeed extends SFXFieldFactory
{
	private static final long serialVersionUID = -8944879051364773954L;

	@Override
	protected SFXBase getInstance()
	{
		SFXValueFollows valueSpeed = new SFXValueFollows();
		
		SFXValue fieldFollows = (SFXValue)getField("Range").getInstance();
		
		// When instance created range already been created
		valueSpeed.createField("Speed").addListeners(m_listFieldListeners).addDepends(fieldFollows);
		valueSpeed.setFollows(fieldFollows).setTable(SFXTable.getDefaultValueTable()).setMultiplierTable(new SFXSpeed()).setValue(0);
		return replaceParent(valueSpeed);
	}
}

class SFXFieldFactoryDuration extends SFXFieldFactory
{
	private static final long serialVersionUID = 855916558508563728L;

	@Override
	protected SFXBase getInstance()
	{
		SFXValue valueDuration = new SFXValue();
		valueDuration.createField("Duration").addListeners(m_listFieldListeners);
		valueDuration.setTable(SFXTable.getDefaultValueTable()).setMultiplierTable(new SFXTime()).setValue(0);
		return replaceParent(valueDuration);
	}
}

class SFXFieldFactorySubMandatory extends SFXFieldFactory
{
	private static final long serialVersionUID = 2094482544817065577L;

	@Override
	protected SFXBase getInstance()
	{
		SFXCalculated calculatedSubMandatory = new SFXFieldSubMandatory();
		calculatedSubMandatory.createField("SubMandatory").addListeners(m_listFieldListeners);
		setDependencies(calculatedSubMandatory);
		return replaceParent(calculatedSubMandatory);
	}
	
	protected SFXBase setDependencies(SFXBase fieldSource)
	{
		String[] ascDepends = {"Effect", "Range", "Speed", "Duration"};
		for (int iIndex = 0; iIndex<ascDepends.length; ++iIndex)
		{
			SFXBase field = getField(ascDepends[iIndex]);
			fieldSource.addDepends(field);
		}
		return fieldSource;	
	}
}

class SFXFieldFactoryHalfMandatory extends SFXFieldFactorySubMandatory
{
	private static final long serialVersionUID = 2094482544817065577L;

	@Override
	protected SFXBase getInstance()
	{
		SFXCalculated calculatedHalfMandatory = new SFXFieldHalfMandatory();				
		calculatedHalfMandatory.createField("HalfMandatory").addListeners(m_listFieldListeners);
		setDependencies(calculatedHalfMandatory);
		return replaceParent(calculatedHalfMandatory);
	}
}

class SFXFieldFactoryCasting extends SFXFieldFactory
{
	private static final long serialVersionUID = -8317686572859496180L;

	@Override
	protected SFXBase getInstance()
	{
		SFXValue valueCasting = new SFXValue();
		valueCasting.createField("Casting").addListeners(m_listFieldListeners);
		valueCasting.setTable(SFXTable.getDefaultValueTable()).setMultiplierTable(new SFXTime()).setValue(0);
		return replaceParent(valueCasting);
	}
}

class SFXFieldFactoryMandatory extends SFXFieldFactory
{
	private static final long serialVersionUID = 7818074985939535045L;

	@Override
	protected SFXBase getInstance()
	{
		SFXCalculated calculatedMandatory = new SFXFieldMandatory();
		calculatedMandatory.createField("Mandatory").addListeners(m_listFieldListeners);

		SFXBase fieldSubMandatory = getField("SubMandatory");
		SFXBase fieldCasting = getField("Casting");
		calculatedMandatory.addDepends(fieldSubMandatory).addDepends(fieldCasting);
		// Also add source for 1/2 mandatory for calculation
		SFXBase fieldHalf = getField("HalfMandatory");
		calculatedMandatory.m_listSource.add(fieldHalf);
		
		return replaceParent(calculatedMandatory);
	}	
}

class SFXFieldFactoryArea extends SFXFieldFactory
{
	private static final long serialVersionUID = 2539935455802284050L;

	@Override
	protected SFXBase getInstance()
	{
		SFXCompositeArea compositeArea = new SFXCompositeArea();
		// Need to add listeners first since used in create for contained fields
		//compositeArea.createField("AreaEffect", m_listFieldListeners, SFXTable.getDefaultValueTable()).addListeners(m_listFieldListeners);
		compositeArea.createField("AreaEffect").addListeners(m_listFieldListeners);
		compositeArea.addFirst(SFXTable.getDefaultValueTable());
		return replaceParent(compositeArea);
	}	
}

class SFXFieldFactoryMultiTarget extends SFXFieldFactory
{
	private static final long serialVersionUID = -5188090143057991699L;

	@Override
	protected SFXBase getInstance()
	{
		SFXFieldMultiTarget scalarMultiTarget = new SFXFieldMultiTarget();
		scalarMultiTarget.createField("MultiTarget").addListeners(m_listFieldListeners);

		SFXCompositeArea fieldArea = (SFXCompositeArea)getField("AreaEffect");
		scalarMultiTarget.setSource(fieldArea).addDepends(fieldArea);
		
		return replaceParent(scalarMultiTarget);
	}
}

class SFXFieldFactoryMultiAttribute extends SFXFieldFactory
{
	private static final long serialVersionUID = 196917342265751124L;

	@Override
	protected SFXBase getInstance()
	{
		SFXValue valueMultiAttribute = new SFXValue();
		valueMultiAttribute.createField("MultiAttribute").addListeners(m_listFieldListeners);
		valueMultiAttribute.setTable(SFXTable.getDefaultValueTable()).setMultiplierTable(new SFXMultiAttribute()).setValue(0);	
		return replaceParent(valueMultiAttribute);
	}
}

class SFXFieldFactoryChange extends SFXFieldFactory
{
	private static final long serialVersionUID = -8176314284298481944L;

	@Override
	protected SFXBase getInstance()
	{
		SFXFieldChangeTarget scalarChangeTarget = new SFXFieldChangeTarget();
		scalarChangeTarget.createField("Change target").addListeners(m_listFieldListeners);
		
		SFXCompositeArea fieldArea = (SFXCompositeArea)getField("AreaEffect");
		SFXScalar scalarTarget = (SFXScalar)getField("MultiTarget");
		SFXValue valueAttributes = (SFXValue)getField("MultiAttribute");

		scalarChangeTarget.addDepends(fieldArea).addDepends(scalarTarget).addDepends(valueAttributes);
		scalarChangeTarget.setSources(fieldArea, scalarTarget, valueAttributes);
		
		return replaceParent(scalarChangeTarget);
	}	
}

class SFXFieldFactoryVariableEffect extends SFXFieldFactory
{
	private static final long serialVersionUID = 1L;

	@Override
	protected SFXBase getInstance()
	{
		SFXScalar scalarVariableEffect = new SFXFieldVariableEffect();
		scalarVariableEffect.createField("Variable Effect").addListeners(m_listFieldListeners);
		return replaceParent(scalarVariableEffect);
	}	
}

class SFXFieldFactoryVariableDuration extends SFXFieldFactory
{
	private static final long serialVersionUID = -7640081920623063403L;

	@Override
	protected SFXBase getInstance()
	{
		SFXComposite compositeVariableDuration = new SFXFieldVariableDuration();
		compositeVariableDuration.addListeners(m_listFieldListeners).createField("Variable Duration");
		return replaceParent(compositeVariableDuration);
	}		
}

class SFXFieldFactoryApportation extends SFXFieldFactory
{
	private static final long serialVersionUID = -4789406230614758682L;
	
	@Override
	protected SFXBase getInstance()
	{
		SFXComposite compositeApportation = new SFXFieldApportation();
		compositeApportation.addListeners(m_listFieldListeners).createField("Apportation");
		
		SFXValueMove fieldMove = (SFXValueMove) compositeApportation.getField(SFXFieldApportation._MOVE);
		
		String[] ascDepends = 
		{
			"Mandatory", "AreaEffect", "MultiTarget", "MultiAttribute",
			"ChangeTarget", "VariableEffect", "VariableDuration",
		};
		
		for (int iIndex = 0; iIndex<ascDepends.length; ++iIndex)
		{
			SFXBase field = getField(ascDepends[iIndex]);
			fieldMove.addDepends(field);
		}
		
		SFXScalar scalarEffect = (SFXScalar)getField("Effect");
		fieldMove.setFollows(scalarEffect);

		return replaceParent(compositeApportation);
	}			
}

class SFXFieldFactoryMaintenance extends SFXFieldFactory
{
	private static final long serialVersionUID = -7896809407281953130L;
	
	@Override
	protected SFXBase getInstance()
	{
		SFXCalculated calculateMaintenance = new SFXFieldMaintenance();
		calculateMaintenance.createField("Maintenance").addListeners(m_listFieldListeners);
		
		String[] ascDepends = 
		{
			"Mandatory", "AreaEffect", "MultiTarget", "MultiAttribute",
			"ChangeTarget", "VariableEffect", "VariableDuration", "Apportation"
		};
		
		for (int iIndex = 0; iIndex<ascDepends.length; ++iIndex)
		{
			SFXBase field = getField(ascDepends[iIndex]);
			calculateMaintenance.addDepends(field);
		}
		
		return replaceParent(calculateMaintenance);
	}
}

class SFXFieldFactoryFocus extends SFXFieldFactory
{
	private static final long serialVersionUID = 154379852812210210L;

	@Override
	protected SFXBase getInstance()
	{
		SFXCalculated calculatedFocus = new SFXFieldFocus();
		calculatedFocus.createField("Focus").addListeners(m_listFieldListeners);
		
		SFXBase fieldEffect = getField("Effect");
		SFXBase fieldDuration = getField("Duration");
		
		calculatedFocus.addDepends(fieldEffect).addDepends(fieldDuration);
		return replaceParent(calculatedFocus);
	}
}

class SFXFieldFactoryCharges extends SFXFieldFactory
{
	private static final long serialVersionUID = -5801086271196050296L;

	@Override
	protected SFXBase getInstance()
	{
		SFXFieldCharges valueCharges = new SFXFieldCharges();
		valueCharges.createField("Charges").addListeners(m_listFieldListeners);
		return replaceParent(valueCharges);
	}	
}

class SFXFieldFactoryOptional extends SFXFieldFactory
{
	private static final long serialVersionUID = -9033202783297341196L;

	@Override
	protected SFXBase getInstance()
	{
		SFXCalculated valueOptional = new SFXFieldOptional();
		valueOptional.createField("Optional").addListeners(m_listFieldListeners);
		
		String[] ascDepends = 
		{
			"AreaEffect", "MultiTarget", "MultiAttribute",
			"ChangeTarget", "VariableEffect", "VariableDuration",
			"Apportation", "Maintenance", "Focus", "Charges"
		};
		
		for (int iIndex = 0; iIndex<ascDepends.length; ++iIndex)
		{
			SFXBase field = getField(ascDepends[iIndex]);
			valueOptional.addDepends(field);
		}
		
		return replaceParent(valueOptional);
	}
}

class SFXFieldFactoryCommunity extends SFXFieldFactory
{
	private static final long serialVersionUID = -5852429815232859723L;
	
	@Override
	protected SFXBase getInstance()
	{
		SFXCompositeCommunity compositeCommunity = new SFXCompositeCommunity();
		compositeCommunity.createField("Community").addListeners(m_listFieldListeners);
		compositeCommunity.addFirst(new SFXFieldCommunity());
		return replaceParent(compositeCommunity);
	}	
}

class SFXFieldFactoryComponents extends SFXFieldFactory
{
	private static final long serialVersionUID = 8033376805154925868L;
	
	@Override
	protected SFXBase getInstance()
	{
		SFXCompositeComponents compositeComponents = new SFXCompositeComponents();
		compositeComponents.createField("Components").addListeners(m_listFieldListeners);
		
		String[] ascDepends = 
		{
			"Mandatory", "Optional", "Fraction"
		};
		
		for (int iIndex = 0; iIndex<ascDepends.length; ++iIndex)
		{
			SFXBase field = getField(ascDepends[iIndex]);
			compositeComponents.addDepends(field);
		}
		
		compositeComponents.addFirst(new SFXFieldComponent());
		return replaceParent(compositeComponents);
	}
	
}

class SFXFieldFactoryConcentration extends SFXFieldFactory
{
	private static final long serialVersionUID = 6988493015702845973L;

	@Override
	protected SFXBase getInstance()
	{
		SFXValueModifier valueConcentration = new SFXFieldConcentration();
		valueConcentration.createField("Concentration").addListeners(m_listFieldListeners);
		valueConcentration.setTable(SFXTable.getDefaultValueTable()).setMultiplierTable(new SFXTime()).setValue(0);
		return replaceParent(valueConcentration);
	}
}

class SFXFieldFactoryGestures extends SFXFieldFactory
{
	private static final long serialVersionUID = -2138640423864010392L;

	@Override
	protected SFXBase getInstance()
	{
		SFXCompositeGestures compositeGestures = new SFXCompositeGestures();
		compositeGestures.createField("Gestures").addListeners(m_listFieldListeners);		
		compositeGestures.addFirst(new SFXFieldGesture());
		return replaceParent(compositeGestures);
	}
}

class SFXFieldFactoryIncantation extends SFXFieldFactory
{
	private static final long serialVersionUID = -1818687154379413674L;

	@Override
	protected SFXBase getInstance()
	{
		SFXFieldIncantation valueIncantation = new SFXFieldIncantation();
		valueIncantation.createField("Incantation").addListeners(m_listFieldListeners);
		valueIncantation.setTable(SFXTable.getDefaultValueTable()).setMultiplierTable(new SFXIncantation()).setValue(0);
		return replaceParent(valueIncantation);
	}	
}

class SFXFieldFactoryRelatedSkill extends SFXFieldFactory
{
	private static final long serialVersionUID = -8750111782571938436L;

	@Override
	protected SFXBase getInstance()
	{
		SFXFieldRelatedSkill valueRelatedSkill = new SFXFieldRelatedSkill();
		valueRelatedSkill.createField("RelatedSkill").addListeners(m_listFieldListeners);
		valueRelatedSkill.setTable(SFXTable.getDefaultValueTable()).setMultiplierTable(new SFXRelatedSkill()).setValue(0);
		return replaceParent(valueRelatedSkill);
	}		
}

class SFXFieldFactoryOther extends SFXFieldFactory
{
	private static final long serialVersionUID = 8277963525622354305L;

	@Override
	protected SFXBase getInstance()
	{
		SFXDouble calculatedOthers = new SFXDouble();
		calculatedOthers.createField("Others").addListeners(m_listFieldListeners);
		return replaceParent(calculatedOthers);
	}
}

class SFXFieldFactoryUnreal extends SFXFieldFactory
{
	private static final long serialVersionUID = -3131671198649210806L;

	@Override
	protected SFXBase getInstance()
	{
		SFXFractionUnreal fractionUnreal = new SFXFractionUnreal();
		fractionUnreal.createField("Unreal").addListeners(m_listFieldListeners);

		SFXBase fieldEffect = getField("Effect");
		fractionUnreal.addDepends(fieldEffect);
		
		return replaceParent(fractionUnreal);
	}	
}

class SFXFieldFactoryModifiers extends SFXFieldFactory
{
	private static final long serialVersionUID = 7008825902152114452L;

	@Override
	protected SFXBase getInstance()
	{
		SFXFieldModifiers calculatedModifiers = new SFXFieldModifiers();
		calculatedModifiers.createField("Modifiers").addListeners(m_listFieldListeners);
		
		String[] ascDepends = 
		{
			"Mandatory", "Optional", "Fraction",
			"Community", "Components", "Concentration", "Gestures", "Incantation", "RelatedSkill", "Other", "Unreal"
		};
		
		for (int iIndex = 0; iIndex<ascDepends.length; ++iIndex)
		{
			SFXBase field = getField(ascDepends[iIndex]);
			calculatedModifiers.addDepends(field);
		}		
		
		return replaceParent(calculatedModifiers);
	}
}

class SFXFieldFactoryFraction extends SFXFieldFactory
{
	private static final long serialVersionUID = -4979974645177709501L;

	@Override
	protected SFXBase getInstance()
	{
		SFXFraction fraction = new SFXFraction();
		fraction.createField("Fraction").addListeners(m_listFieldListeners);
		return replaceParent(fraction);
	}
}

class SFXFieldFactoryTotal extends SFXFieldFactory
{
	private static final long serialVersionUID = 4634206042226661250L;

	@Override
	protected SFXBase getInstance()
	{
		SFXFieldTotal calculatedTotal = new SFXFieldTotal();
		calculatedTotal.createField("Totals").addListeners(m_listFieldListeners);
		
		SFXCalculated calculatedMandatory = (SFXCalculated)getField("Mandatory");
		SFXCalculated calculatedOptional = (SFXCalculated)getField("Optional");
		SFXFraction fraction = (SFXFraction)getField("Fraction");
		SFXDouble doubleModifiers = (SFXDouble)getField("Modifiers");
		
		calculatedTotal.addDepends(calculatedMandatory).addDepends(calculatedOptional).addDepends(fraction).addDepends(doubleModifiers);
		calculatedTotal.setSources(calculatedMandatory, calculatedOptional, fraction, doubleModifiers);
		
		return replaceParent(calculatedTotal);
	}
}

class SFXFieldFactorySkill extends SFXFieldFactory
{
	private static final long serialVersionUID = -7928998553610558398L;

	@Override
	protected SFXBase getInstance()
	{
		SFXValue valueSkill = new SFXValue();
		valueSkill.createField("Skill").addListeners(m_listFieldListeners);
		valueSkill.setTable(SFXTable.getDefaultValueTable()).setMultiplierTable(new SFXSkill());
		return replaceParent(valueSkill);
	}
}

class SFXFieldFactoryConcentrationAdd extends SFXFieldFactory
{
	private static final long serialVersionUID = -360629089894525594L;

	@Override
	protected SFXBase getInstance()
	{
		SFXFieldConcentrationAdd calculateConcentrationAdd = new SFXFieldConcentrationAdd();
		calculateConcentrationAdd.createField("ConcentrationAdd").addListeners(m_listFieldListeners);
		
		SFXBase field = getField("Concentration");
		calculateConcentrationAdd.addDepends(field);
		calculateConcentrationAdd.setConcentration(field);
		
		return replaceParent(calculateConcentrationAdd);
	}
}

class SFXFieldFactoryReception extends SFXFieldFactory
{
	private static final long serialVersionUID = -7026181052410111666L;

	@Override
	protected SFXBase getInstance()
	{
		SFXFieldReception valueReception = new SFXFieldReception();
		valueReception.createField("Reception").addListeners(m_listFieldListeners);
		valueReception.setTable(SFXTable.getDefaultValueTable()).setMultiplierTable(new SFXReception());
		
		SFXBase field = getField("Skill");
		valueReception.addDepends(field);
		valueReception.setSkill(field);
		
		return replaceParent(valueReception);
	}
}

class SFXFieldFactoryTrance extends SFXFieldFactory
{
	private static final long serialVersionUID = -5583294640542385722L;

	@Override
	protected SFXBase getInstance()
	{
		SFXFieldTrance valueTrance = new SFXFieldTrance();
		valueTrance.createField("Trance").addListeners(m_listFieldListeners);
		valueTrance.setTable(SFXTable.getDefaultValueTable()).setMultiplierTable(new SFXTrance());

		SFXBase field = getField("Skill");
		valueTrance.addDepends(field);
		valueTrance.setSkill(field);
		
		return replaceParent(valueTrance);
	}
}

class SFXFieldFactoryLock extends SFXFieldFactory
{
	private static final long serialVersionUID = -6358477327899899823L;

	@Override
	protected SFXBase getInstance()
	{
		SFXValue valueLock = new SFXValue();
		valueLock.createField("Lock").addListeners(m_listFieldListeners);
		valueLock.setTable(SFXTable.getDefaultValueTable()).setMultiplierTable(new SFXLock());
		return replaceParent(valueLock);
	}
}

class SFXFieldFactoryCountenance extends SFXFieldFactory
{
	private static final long serialVersionUID = 3374463168615873873L;

	@Override
	protected SFXBase getInstance()
	{
		SFXValue valueCountenance = new SFXValue();
		valueCountenance.createField("Countenance").addListeners(m_listFieldListeners);
		valueCountenance.setTable(SFXTable.getDefaultValueTable()).setMultiplierTable(new SFXCountenance());
		return replaceParent(valueCountenance);
	}
}

class SFXFieldFactoryTotalN extends SFXFieldFactory
{
	private static final long serialVersionUID = -2043599650417711357L;

	@Override
	protected SFXBase getInstance()
	{
		SFXFieldTotalN calculatedTotal = new SFXFieldTotalN();
		calculatedTotal.createField("Totals").addListeners(m_listFieldListeners);
		
		String[] ascDepends = 
		{
			"Mandatory", "Optional", "Fraction", "Modifiers", 
			"Lock", "ConcentrationAdd", "Reception", "Trance", "Countenance"
		};
		
		for (int iIndex = 0; iIndex<ascDepends.length; ++iIndex)
		{
			SFXBase field = getField(ascDepends[iIndex]);
			calculatedTotal.addDepends(field);
		}		
		
		return replaceParent(calculatedTotal);
	}
}

