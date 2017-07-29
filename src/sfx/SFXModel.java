package sfx;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A listener to the model can respond to four model changes
 * Either a field changing {in value}
 * A field being added
 * A field being removed or
 * A field being replaced.
 * The last event happens when data is loaded or the model is reset with 'new'
 */

interface SFXModelListener 
{
	public boolean addField(SFXBase fieldAdded);
	public boolean changedField(SFXBase field);
	public boolean replaceField(SFXBase fieldReplace, SFXBase fieldWith);
	public boolean removeField(SFXBase fieldRemoved);
}

public class SFXModel implements Serializable 
{
	private static final long serialVersionUID = -1193501929541930535L;
	
	private transient List<SFXModelListener> m_listModelListeners = null; // Marked as transient so do not require to save listeners
	private Map<String, SFXBase> m_mapFields = null;
		
	public SFXModel createModel() 
	{
		m_listModelListeners = new CopyOnWriteArrayList<SFXModelListener>();
		m_mapFields = createFields();
		return this;
	}
	
	/* ======================================================================
	   Listeners for the model
	   ====================================================================== */

	public void addListener(SFXModelListener listener) 
	{
		m_listModelListeners.add(listener);
	}
	
	public void removeListener(SFXModelListener listener)
	{
		if (m_listModelListeners.contains(listener)) m_listModelListeners.remove(listener);
	}
	
	/* ======================================================================
	   Create the model fields.  Structure of model data is an array of 
	   SFXFields which could also include an SFXComposite field of fields
	   
	   Field dependency is handled using a factory holder for the field that
	   then creates the specific field when there is an access to the model
	   e.g. a view uses getField(_OPTIONAL) to obtain the sub-total of the 
	   optional steps.  The array has a factory holder for _OPTIONAL so when
	   the getField requests the getInstance of the SFXField the SFXField is
	   constructed for the optional sub-total; but this is dependent on (lots)
	   of other fields, so they are requested in turn which makes their 
	   factory holder trigger the getInstance.  When the real field replaces
	   the factory holder the standard implementation of SFX getInstance 
	   returns itself
	   ====================================================================== */

	private Map<String, SFXBase> createFields()
	{
		Map<String, SFXBase> mapFields = new ConcurrentHashMap<String, SFXBase>();
		
		SFXMapContent[] aClassFactory = 
		{
			new SFXMapContent(SFXFieldFactory.class, "Name"),
			
			new SFXMapContent(SFXFieldFactoryEffect.class, "Effect"), 	
			new SFXMapContent(SFXFieldFactoryRange.class, "Range"), 	
			new SFXMapContent(SFXFieldFactorySpeed.class, "Speed"), 	
			new SFXMapContent(SFXFieldFactoryDuration.class, "Duration"), 	
			new SFXMapContent(SFXFieldFactorySubMandatory.class, "SubMandatory"),
			new SFXMapContent(SFXFieldFactoryHalfMandatory.class, "HalfMandatory"), 	
			new SFXMapContent(SFXFieldFactoryCasting.class, "Casting"), 	
			new SFXMapContent(SFXFieldFactoryMandatory.class, "Mandatory"),
			
			new SFXMapContent(SFXFieldFactoryArea.class, "AreaEffect"),
			new SFXMapContent(SFXFieldFactoryMultiTarget.class, "MultiTarget"),
			new SFXMapContent(SFXFieldFactoryMultiAttribute.class, "MultiAttribute"),
			new SFXMapContent(SFXFieldFactoryChange.class, "ChangeTarget"),
			new SFXMapContent(SFXFieldFactoryVariableEffect.class, "VariableEffect"),
			new SFXMapContent(SFXFieldFactoryVariableDuration.class, "VariableDuration"),
			new SFXMapContent(SFXFieldFactoryApportation.class, "Apportation"),
			new SFXMapContent(SFXFieldFactoryMaintenance.class, "Maintenance"),
			new SFXMapContent(SFXFieldFactoryFocus.class, "Focus"),
			new SFXMapContent(SFXFieldFactoryCharges.class, "Charges"),
			new SFXMapContent(SFXFieldFactoryOptional.class, "Optional"),
			
			new SFXMapContent(SFXFieldFactoryCommunity.class, "Community"),
			new SFXMapContent(SFXFieldFactoryComponents.class, "Components"),
			new SFXMapContent(SFXFieldFactoryConcentration.class, "Concentration"),
			new SFXMapContent(SFXFieldFactoryGestures.class, "Gestures"),
			new SFXMapContent(SFXFieldFactoryIncantation.class, "Incantation"),
			new SFXMapContent(SFXFieldFactoryRelatedSkill.class, "RelatedSkill"),
			new SFXMapContent(SFXFieldFactoryOther.class, "Other"),
			new SFXMapContent(SFXFieldFactoryUnreal.class, "Unreal"),
			new SFXMapContent(SFXFieldFactoryModifiers.class, "Modifiers"),
			
			new SFXMapContent(SFXFieldFactoryFraction.class, "Fraction"),
			new SFXMapContent(SFXFieldFactoryTotal.class, "Total"),
			
			new SFXMapContent(SFXFieldFactorySkill.class, "Skill"),
			new SFXMapContent(SFXFieldFactoryConcentrationAdd.class, "ConcentrationAdd"),
			new SFXMapContent(SFXFieldFactoryReception.class, "Reception"),
			new SFXMapContent(SFXFieldFactoryTrance.class, "Trance"),
			new SFXMapContent(SFXFieldFactoryLock.class, "Lock"),
			new SFXMapContent(SFXFieldFactoryCountenance.class, "Countenance"),
			new SFXMapContent(SFXFieldFactoryTotalN.class, "Specific"),
		};
		
		for (int iIndex = 0; iIndex<aClassFactory.length; ++iIndex)
		{
			try 
			{
				SFXMapContent build = aClassFactory[iIndex];
				SFXFieldFactory factory = (SFXFieldFactory)build.m_classFactory.newInstance();
				SFXBase field = factory.setFields(mapFields).createField(aClassFactory[iIndex].m_scField).addListeners(m_listModelListeners);
				
				mapFields.put(build.m_scField, field);
			} 
			catch (InstantiationException x) 
			{
				throw new RuntimeException(x);
			} 
			catch (IllegalAccessException x) 
			{
				throw new RuntimeException(x);
			}
		}
		
		return mapFields;
	}
	
	public SFXBase getField(String scField)
	{
		return m_mapFields.get(scField).getInstance();
	}
	
	/**
	 * Replacing the content is a very loopy kind of thing
	 * First all the fields that are going to be replaced are given the 
	 * listeners of the model. 
	 * Then each (loop) listener is told of the replacement of field Replace by field With
	 * Which in itself removes all content (loop) telling all the listeners (loop) that that content is removed
	 * Replacing the actual field Replace with field With
	 * Then adding in any content (loop) telling all the listeners (loop) of the new content
	 */

	public boolean replaceContent(SFXModel loadModel) 
	{
		// The model internal references {dependencies follows etc.,.} should already be set up correctly
		Map<String, SFXBase> mapField = loadModel.m_mapFields;
		
		for (Map.Entry<String, SFXBase> entry : mapField.entrySet())
		{
			// Make sure field is created {for new these will still be the factory}
			SFXBase field = entry.getValue().getInstance();
			
			// Set the field listeners same as for all the fields
			field.addListeners(m_listModelListeners);

			// Tell the listeners that the field has changed
			replaceContentListeners(getField(entry.getKey()), field);
		}
		
		m_mapFields = mapField;
		
		return true;
	}
	
	private boolean replaceContentListeners(SFXBase fieldReplace, SFXBase fieldWith)
	{
		// If the field has content then remove the content ...
		removeContentListeners(fieldReplace);
		
		// Tell the listeners that the field has changed
		for (SFXModelListener listener : m_listModelListeners)
		{
			// ... replace the field
			listener.replaceField(fieldReplace, fieldWith);
		}
		
		// ... and then if the new field has content add the listeners to this content
		addContentListeners(fieldWith);
		
		return true;
	}
	
	private boolean removeContentListeners(SFXBase fieldParent)
	{
		if (!(fieldParent instanceof SFXComposite)) return false;
		
		SFXComposite compositeRemove = (SFXComposite) fieldParent;
		List<SFXBase> listFields = compositeRemove.getComposition();
		
		for (SFXBase fieldRemove: listFields)
		{
			removeContent(fieldRemove);
		}
		
		return true;
	}
	
	private boolean addContentListeners(SFXBase fieldParent)
	{
		if (!(fieldParent instanceof SFXComposite)) return false;
		
		SFXComposite compositeAdd = (SFXComposite) fieldParent;
		List<SFXBase> listFields = compositeAdd.getComposition();
		
		for (SFXBase fieldAdd: listFields)
		{
			// Set the field listeners same as for all the fields
			fieldAdd.addListeners(m_listModelListeners);
			addContent(fieldAdd);
		}
		
		return true;
	}
	
	private boolean removeContent(SFXBase fieldRemoved)
	{
		// Tell the listeners that the field has been removed
		for (SFXModelListener listener : m_listModelListeners)
		{
			listener.removeField(fieldRemoved);
		}		
		return true;				
	}
	
	private boolean addContent(SFXBase fieldAdded)
	{
		// Tell the listeners that the field has been removed
		for (SFXModelListener listener : m_listModelListeners)
		{
			listener.addField(fieldAdded);
		}		
		return true;				
	}
}

/* ==========================================================================
   Construct model from factory types to avoid reference to undefined fields
   ========================================================================== */

class SFXMapContent
{
	Class<? extends SFXBase> m_classFactory = null;
	String m_scField = null;
	
	public SFXMapContent(Class<? extends SFXBase> classFactory, String scField)
	{
		m_classFactory = classFactory;
		m_scField = scField;
	}
}

