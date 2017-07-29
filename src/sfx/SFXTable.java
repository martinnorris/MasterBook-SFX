package sfx;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class SFXTable implements Serializable
{
	private static final long serialVersionUID = -6779394639303826048L;

	private static SFXTable sm_tableDefault = null;
	
	public static SFXTable getDefaultValueTable()
	{
		if (null==sm_tableDefault)
		{
			sm_tableDefault = new SFXTable();
			sm_tableDefault.setInitialValues(100);			
		}
		return sm_tableDefault;
	}
	
	protected List<SFXTableEntry> m_listEntries = null;
	
	public SFXTable setInitialValues(int iRange) 
	{
		m_listEntries = new ArrayList<SFXTableEntry>();
		
		// Cost goes up linearly while value goes up exponentially
		float[] aiValues = {1, 1.5f, 2.5f, 4, 6};
		int iMultiplier = 1;
		
		for (int iCost = 0; iCost<iRange;)
		{
			float fValue = aiValues[iCost % aiValues.length] * iMultiplier;
			
			SFXTableEntry entry = new SFXTableEntry();
			entry.setValue(iCost, fValue);
			
			m_listEntries.add(entry);
			
			if (0==(++iCost % aiValues.length)) iMultiplier *= 10; // e.g. range 1.. 10.. 100.. going up in aiValue steps e.g. 1000 1500 2500 4000 6000 10000
		}
		
		return this;
	}
	
	public String[] getContent() 
	{
		String[] ascValues = new String[m_listEntries.size()];
		int iIndex = 0;
		
		for (SFXTableEntry entry : m_listEntries)
			ascValues[iIndex++] = Float.toString(entry.getValue());
			
		return ascValues;
	}

	public float getValue(int iEntry) 
	{
		return m_listEntries.get(iEntry).getValue();
	}

	public int getCost(int iEntry) 
	{
		return m_listEntries.get(iEntry).getCost();
	}

	public boolean outRange(int iValue) 
	{
		if (0>iValue) return true;
		if (m_listEntries.size()<=iValue) return true;
		return false;
	}

	public int closestValue(float fValue) 
	{
		for (SFXTableEntry entry : m_listEntries)
			if (fValue<=entry.getValue()) return entry.getCost(); // Cost is the same linear value as the index in the table
		return -1;
	}
}

/* ==========================================================================
   Table values giving a cross reference of value to cost
   The cost is simply the linear index of the entry in the table e.g. row 0 cost 0f
   row 10 cost 10; while the entry is exponentially increasing e.g. 1, 1.5, 2.5, 4, 6, 10, ...

   All fields share the same table since they use their own index into the table
   ========================================================================== */

class SFXTableEntry implements Serializable
{
	private static final long serialVersionUID = -3614689936735109021L;

	private int m_iCost = 0;
	private float m_fValue = 1.0f;
	
	public void setValue(int iCost, float fValue) 
	{
		m_iCost = iCost;
		m_fValue = fValue;
	}
	
	public int getCost() 
	{
		return m_iCost;
	}
	
	public float getValue()
	{
		return m_fValue;
	}
}

/* ==========================================================================
   special table used for different value presentation used by community
   ========================================================================== */

class SFXTableCommunity extends SFXTable
{
	private static final long serialVersionUID = -8002572417232253197L;

	@Override
	public String[] getContent() 
	{
		String[] ascValues = new String[m_listEntries.size()];
		int iIndex = 0;
		int iLast = 0;
		boolean zBeginning = true;
		
		// Create 1, 2,  3-4, 5-6, 7-10, 11-15
		// From   1  1.5 2.5  4    6
		
		// Up     1  2   3    4    6
		// +1     2  3   4    5    7
		// n->n+1 =  =   =    5-6  7-10
		// when = 1  2   3  <
		// push back     3-4
		
		for (SFXTableEntry entry : m_listEntries)
		{
			// Create 1, 2,  3-4, 5-6, 7-10, 11-15
			// From   1  1.5 2.5  4    6
			double dfNext = entry.getValue();
			// From   1  1.5 2.5  4    6			
			// Ceil   1  2   3    4    6
			dfNext = Math.ceil(dfNext);
			int iNext = (int)dfNext;
			
			// Last   0  1  2   3    4    6
			// Next   1  2  3   4    6    10
			// Range  1  1  1   1    2    4
			int iRange = iNext - iLast;
			
			if (1==iRange) 
			{
				// Last  0  1  2   3
				ascValues[iIndex++] = Integer.toString(iLast);
				iLast = iNext;
				continue;
			}
			
			if (zBeginning)
			{
				// In the beginning when just have 0, 1, 2, 3 when switch to 5-6 need to retroactively add 3-4
				zBeginning = false;
				// Last  4
				// Entry 3-4
				ascValues[iIndex-1] = String.format("%d-%d", iLast - 1, iLast);
			}

			// Last   4    6
			// Next   6    10
			// Entry  5-6  7-10
			ascValues[iIndex++] = String.format("%d-%d", iLast+1, iNext);
			iLast = iNext;
		}
			
		return ascValues;
	}
}

class SFXBonus extends SFXTable
{
	private static final long serialVersionUID = 6863442313751414990L;

	@Override
	public String[] getContent() 
	{
		String[] ascValues = new String[] {"no bonus", "linked (+1)", "appropriate (+2)", "inventive (+3)"};
		return ascValues;
	}
}

/* ==========================================================================
   Table multipliers are used for values
   They have a list of values and names and an index to show which they are using.  
   Each name corresponds to an offset value

   Each field using a multiplier has an instance of the expected type
   ========================================================================== */

class SFXTableMultiplier implements Serializable
{
	private static final long serialVersionUID = 4957172902341654860L;

	private int m_iIndex = 0;
	
	protected int[] m_aiOffsets = null;
	protected String[] m_scNames = null;
	
	public int getIndex()
	{
		return m_iIndex;
	}
	
	public SFXTableMultiplier setIndex(int iMultiplier) 
	{
		if (0>iMultiplier) return this;
		if (m_aiOffsets.length<=iMultiplier) return this;
		m_iIndex = iMultiplier;
		return this;
	}

	public int getOffset()
	{
		return m_aiOffsets[m_iIndex];
	}
	
	public String getName()
	{
		return m_scNames[m_iIndex];
	}
	
	public String[] getContent() 
	{
		return m_scNames;
	}
}

/** Time extends the table multiplier to give different units for time measurement */

class SFXTime extends SFXTableMultiplier
{
	private static final long serialVersionUID = -8419055773135666425L;

	private static final int[] sm_aiTimeOffsets = {0, 9, 18, 25, 29, 32, 38};
	private static final String[] sm_scTimeNames = {"seconds", "minutes", "hours", "days", "months", "years"};
	
	public SFXTime()
	{
		m_aiOffsets = sm_aiTimeOffsets;
		m_scNames = sm_scTimeNames;
	}
}

class SFXDistance extends SFXTableMultiplier
{
	private static final long serialVersionUID = 4122606509311309514L;

	private static final int[] sm_aiDistanceOffsets = {0, 15, -3, 16, 10, 11, 23, 30, 32, 33, 34, 35, 38};
	private static final String[] sm_scDistanceNames = {"meters", "kilometers", "feet", "miles", "football field", "city block", "marathon race", "length of state", "Paris to Moscow", "NY to LA", "NY to London", "London to Tokyo", "Round the World"};

	public SFXDistance()
	{
		m_aiOffsets = sm_aiDistanceOffsets;
		m_scNames = sm_scDistanceNames;
	}
}

class SFXSpeed extends SFXTableMultiplier
{
	private static final long serialVersionUID = -2275867279170799981L;

	private static final int[] sm_aiSpeedOffsets = {0, 2, 3};
	private static final String[] sm_scSpeedNames = {"meters per round", "kph", "mph"};
	
	public SFXSpeed()
	{
		m_aiOffsets = sm_aiSpeedOffsets;
		m_scNames = sm_scSpeedNames;
	}
}

class SFXWeight extends SFXTableMultiplier
{
	private static final long serialVersionUID = -2275867279170799981L;

	private static final int[] sm_aiWeightOffsets = {0, -2, 15};
	private static final String[] sm_scWeightNames = {"kg", "pounds", "tons"};
	
	public SFXWeight()
	{
		m_aiOffsets = sm_aiWeightOffsets;
		m_scNames = sm_scWeightNames;
	}
}

/* ==========================================================================
   Specialisations for tables - mostly where table is a drop down choice of
   items for example sitches which can be none off off/on
   ========================================================================== */

class SFXShape extends SFXTableMultiplier
{
	private static final long serialVersionUID = -3751957516200493376L;

	private static final int[] sm_aiShapeOffsets = {0, 1, 3, 6};
	private static final String[] sm_scShapeNames = {"none", "single", "any", "fluid"};
	
	public SFXShape()
	{
		m_aiOffsets = sm_aiShapeOffsets;
		m_scNames = sm_scShapeNames;
	}
}

class SFXSwitch extends SFXTableMultiplier
{
	private static final long serialVersionUID = -4656601466440078445L;

	private static final int[] sm_aiSwitchOffsets = {0, 4, 8};
	private static final String[] sm_scSwitchNames = {"none", "off", "off/on"};
	
	public SFXSwitch()
	{
		m_aiOffsets = sm_aiSwitchOffsets;
		m_scNames = sm_scSwitchNames;
	}
}

class SFXCommunityModifier extends SFXTableMultiplier
{
	private static final long serialVersionUID = -8678627936039069002L;

	private static final int[] sm_aiCommunityOffsets = {1, 2, 3, 4, 5, 6, 7};
	private static final String[] sm_scCommunityNames = {"Simple", "DN 8", "DN 10", "DN 12", "DN 14", "DN 16", "DN 18"};
	
	public SFXCommunityModifier()
	{
		m_aiOffsets = sm_aiCommunityOffsets;
		m_scNames = sm_scCommunityNames;
	}
}

class SFXComponent extends SFXTableMultiplier
{
	private static final long serialVersionUID = 5175604955710133453L;
	
	private static final int[] sm_aiComponentOffsets = {0, 1, 2, 3, 4, 5, 6, 7};
	private static final String[] sm_scComponentNames = {"unset", "common, free", "common, cheap", "common, affordable", "uncommon, affordable", "rare, expensive", "very rare, expensive", "unique, fabulously expensive"};
	
	public SFXComponent()
	{
		m_aiOffsets = sm_aiComponentOffsets;
		m_scNames = sm_scComponentNames;
	}	
}

class SFXGesture extends SFXTableMultiplier
{
	private static final long serialVersionUID = -883975369148979107L;

	private static final int[] sm_aiGestureOffsets = {0, 1, 2, 3, 4, 5, 6};
	private static final String[] sm_scGestureNames = {"simple", "average", "complex", "very complex", "extremely complex", "downright hard"};
	
	public SFXGesture()
	{
		m_aiOffsets = sm_aiGestureOffsets;
		m_scNames = sm_scGestureNames;
	}	
}

class SFXIncantation extends SFXTableMultiplier
{
	private static final long serialVersionUID = -4647693325610705542L;
	
	private static final int[] sm_aiIncantationOffsets = {0, 1, 2, 3, 4, 5, 6};
	private static final String[] sm_scIncantationNames = {"none", "few words", "sentence", "incantation", "litany", "complex formula", "extensive complex elements"};
	
	public SFXIncantation()
	{
		m_aiOffsets = sm_aiIncantationOffsets;
		m_scNames = sm_scIncantationNames;
	}		
}

class SFXRelatedSkill extends SFXTableMultiplier
{
	private static final long serialVersionUID = -6899057804385130323L;
	private static final int[] sm_aiRelatedOffsets = {0, 1, 2, 3, 4, 5, 6, 7};
	private static final String[] sm_scRelatedNames = {"none", "DN 6", "DN 7", "DN 8", "DN 9", "DN 10", "DN 11", "DN 12"};
	
	public SFXRelatedSkill()
	{
		m_aiOffsets = sm_aiRelatedOffsets;
		m_scNames = sm_scRelatedNames;
	}			
}

class SFXSkill extends SFXTableMultiplier
{
	private static final long serialVersionUID = -8122388421426801332L;

	public final static int _NECROSCOPE = 1;
	public final static int _SHAMAN = 6;

	public final static int _SKILL = 0;
	public final static int _RESULT = 1;
	public final static int _OPPOSITION = 2;
	
	private static final int[] sm_aiSkills = 
	{
			0, 
			_NECROSCOPE, 2, 3, 
			4, 5, _SHAMAN,
			7, 8, 9,
			10,
	};
	
	private static final String[] sm_acSkills = 
	{
		"Unset", 
		"Necroscope", "Necromancer", "Prescient", 
		"Seer", "Sensitive",  "Shaman",  
		"Spotter", "Telepath", "Numerancer",
		"Witch - self",
	};
	
	private static final String[][] sm_aacBasis = 
	{
		{"", "", ""},
		
		{"Charisma", "Interactive", "Confidence"},
		{"Strength", "Interactive", "Willpower"},
		{"Intellect", "Time", ""},
		
		{"Mind", "Time", ""},
		{"Intellect", "Special", ""},
		{"Confidence", "Damage", "Toughness (ignores armour)"},
		
		{"Mind", "Special", ""},
		{"Intellect", "Interactive", "Intellect"},
		{"", "", ""},
		
		{"Confidence", "Push", "Varies"},
	};
	
	public SFXSkill()
	{
		m_aiOffsets = sm_aiSkills;
		m_scNames = sm_acSkills;
	}
	
	public String[] getBasis(int iIndex)
	{
		return sm_aacBasis[iIndex];
	}
}

class SFXReception extends SFXTableMultiplier
{
	private static final long serialVersionUID = 4697788675062000646L;

	private static final int[] sm_aiReception = {0, 1, 2, 3, 4};
	private static final String[] sm_acReception = 
	{
		"Unset",
		"Feeling",
		"INT roll",
		"Perception roll",
		"Automatically knows the effect"
	};
	
	public SFXReception()
	{
		m_aiOffsets = sm_aiReception;
		m_scNames = sm_acReception;
	}
}

class SFXTrance extends SFXTableMultiplier
{
	private static final long serialVersionUID = 1594089871281644464L;

	private static final int[] sm_aiTrance = {0, 1, 2, 3, 4, 5, 6};
	private static final String[] sm_acTrance = 
	{
		"None",
		"Will DN 7 to break trance",
		"Will DN 8 to break trance",
		"Will DN 10 to break trance",
		"Will DN 12 to break trance",
		"Will DN 15 to break trance",
		"Will DN 19 to break trance",
	};
	
	public SFXTrance()
	{
		m_aiOffsets = sm_aiTrance;
		m_scNames = sm_acTrance;
	}
}

class SFXLock extends SFXTableMultiplier
{
	private static final long serialVersionUID = 8826553961101863942L;

	private static final int[] sm_aiLock = {0, 1, 2, 3, 4};
	private static final String[] sm_acLock = 
	{
		"Unset",
		"Target makes Will DN 7 or +2 modifier",
		"Target makes Will DN 8 or +2 modifier",
		"Target makes Will DN 10 or +2 modifier",
		"Target makes Will DN 12 or +2 modifier"
	};
	
	public SFXLock()
	{
		m_aiOffsets = sm_aiLock;
		m_scNames = sm_acLock;
	}
}

class SFXCountenance extends SFXTableMultiplier
{
	private static final long serialVersionUID = 1545524247731722209L;

	private static final int[] sm_aiLook = {0, 1, 2};
	private static final String[] sm_acLook = 
	{
		"Unset",
		"Noticable - grey pallor, foaming mouth",
		"Extreme - convulsions, psychic image",
	};
	
	public SFXCountenance()
	{
		m_aiOffsets = sm_aiLook;
		m_scNames = sm_acLook;
	}
}


