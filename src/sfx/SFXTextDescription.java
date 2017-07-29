package sfx;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * This is a special field for creating the description of the SFX
 * The outline is loaded as HTML and this is parsed to extract which
 * fields from the data are dependencies
 * When one of the dependencies changes then the content of the HTML
 * is updated and the change sent to listeners {to optimise this
 * only fields that have really changed in value propagate the change}
 */

class SFXTextOutline extends SFXBase
{
	private static final long serialVersionUID = -336619503045548783L;

	protected Document m_documentOutline = null;
	protected Map<SFXBase, SFXTextBase> m_mapWrappers = null;
	protected Map<Node, SFXTextBase> m_mapSource = null;
	
	protected SFXViewQuery m_queryView = null;
	protected SFXModel m_dataModel = null;
	protected String m_scFile = null;
	
	protected DocumentBuilder m_builderPopulated = null;
	
	@Override
	public SFXBase createField(String scField)
	{
		// Unlike other fields in the model do not share listeners
		m_listFieldListeners = new CopyOnWriteArrayList<SFXModelListener>();
		
		m_mapWrappers = new ConcurrentHashMap<SFXBase, SFXTextBase>();
		m_mapSource = new ConcurrentHashMap<Node, SFXTextBase>();
		
		return super.createField(scField);
	}

	@Override
	public String getValue()
	{
		Document document = m_builderPopulated.newDocument();
		shallowCopy(document, m_documentOutline.getDocumentElement());
		return transformDocument(document.getDocumentElement());
	}
	
	protected String transformDocument(Node nodeRoot)
	{
		// Convert document to string and return to caller
		
		StringWriter stringTransform = new StringWriter();
		
		try 
		{
			Transformer transformer = TransformerFactory.newInstance().newTransformer();
			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			Result stringStreamResult = new StreamResult(stringTransform);
			Source source = new DOMSource(nodeRoot);
			transformer.transform(source, stringStreamResult);
		} 
		catch (TransformerConfigurationException x) 
		{
			throw new RuntimeException(x);
		} 
		catch (TransformerFactoryConfigurationError x) 
		{
			throw new RuntimeException(x);
		} 
		catch (TransformerException x) 
		{
			throw new RuntimeException(x);
		}
		
		// Transform adds an unnecessary meta node
		String scContent = stringTransform.toString();
		int iMeta = scContent.indexOf("<META");
		int iClose = scContent.indexOf('>', iMeta+1);
		String scStart = scContent.substring(0, iMeta);
		String scEnd = scContent.substring(iClose+1);
		
		return scStart + scEnd;
	}

	/* ======================================================================
	   Load an outline HTML document and find all the <code> nodes
	   These are used to create wrappers with data from the model
	   For checking inclusion and substituting values
	   ====================================================================== */
	
	public SFXTextOutline loadOutline(SFXViewQuery queryView, SFXModel dataModel, String scFile)
	{
		// Remember source arguments to recreate outline
		m_queryView = queryView;
		m_dataModel = dataModel;
		m_scFile = scFile;
		
		try 
		{
			// Want to be able to load the resource from file {normal Eclipse kind of thing} and also from a jar file
			// So load as a stream
			InputStream stream = getClass().getClassLoader().getResourceAsStream(scFile);
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setValidating(false);
			factory.setNamespaceAware(true);
			factory.setIgnoringComments(false);
			factory.setIgnoringElementContentWhitespace(false);
			factory.setExpandEntityReferences(false);
			m_builderPopulated = factory.newDocumentBuilder();
			m_documentOutline = m_builderPopulated.parse(new InputSource(stream));
		} 
		catch (SAXException x) 
		{
			throw new RuntimeException(x);
		} 
		catch (IOException x) 
		{
			throw new RuntimeException(x);
		} 
		catch (ParserConfigurationException x) 
		{
			throw new RuntimeException(x);
		}
		
		return extractFields(m_documentOutline, dataModel);
	}
		
	// Parse the outline to extract the fields and methods to be used
	private SFXTextOutline extractFields(Node nodeRoot, SFXModel dataModel)
	{
		try 
		{
			// Use XPATH to find <code> nodes
			XPathFactory xPathfactory = XPathFactory.newInstance();
			XPath xpath = xPathfactory.newXPath();
			// Create an xpath to find all the code nodes
			XPathExpression xpathExpression = xpath.compile(".//code");
			
			NodeList listCode = (NodeList) xpathExpression.evaluate(nodeRoot, XPathConstants.NODESET);
			
			for (int iLength = listCode.getLength(), iIndex = 0; iIndex<iLength; ++iIndex)
			{
				// Treat each code node			
				Node nodeCode = listCode.item(iIndex);
				SFXTextBase textWrapper = makeWrapper(nodeCode, dataModel);
				m_mapSource.put(nodeCode, textWrapper);
			}			
		} 
		catch (XPathExpressionException x) 
		{
			throw new RuntimeException(x);
		}
		
		return this;
	}
	
	/** Get the text of the code node to construct a wrapper */
	
	private SFXTextBase makeWrapper(Node nodeCode, SFXModel dataModel)
	{
		// Check the parent
		Node nodeParent = nodeCode.getParentNode();
		String scParentName = nodeParent.getNodeName();
		boolean zIsHeader = scParentName.startsWith("h");		
		// Split the node content so can find the field and the accessibility
		String scField = nodeCode.getTextContent();
		return setWrapper(nodeCode, dataModel, null, scField, zIsHeader);
	}
	
	/** Intermediate step if the node is a child node of a model field */
	
	private SFXTextBase setWrapper(Node nodeCode, SFXModel dataModel, SFXBase field, String scField, boolean zIsHeader)
	{
		String[] ascContent = scField.split("[.]", 2); // Need to match a '.'
		
		if (null==field)
		{
			field = dataModel.getField(ascContent[0]);
			return setWrapper(nodeCode, dataModel, field, ascContent[1], zIsHeader);
		}
		
		if (ascContent[0].startsWith("*"))
		{
			return duplicateNode(nodeCode, dataModel, field, ascContent[1], zIsHeader);
		}
		
		if (field instanceof SFXComposite)
		{
			SFXComposite fieldComposite = (SFXComposite) field;
			List<SFXBase> listFields = fieldComposite.getComposition();
			for (SFXBase fieldChild : listFields)
			{
				if (ascContent[0].equals(fieldChild.m_scField)) return setWrapper(nodeCode, dataModel, fieldChild, ascContent[1], zIsHeader);
				// Check against a specific number
				String scIndex = Integer.toString(listFields.indexOf(fieldChild));
				if (ascContent[0].equals(scIndex)) return setWrapper(nodeCode, dataModel, fieldChild, ascContent[1], zIsHeader);
			}
		}
		
		return mapWrapper(field, scField, zIsHeader);		
	}
	
	private SFXTextBase duplicateNode(Node nodeCode, SFXModel dataModel, SFXBase field, String scField, boolean zIsHeader)
	{
		// Modify all nodes at level of parent of nodeCode to have '0' instead of '*'
		SFXComposite fieldComposite = (SFXComposite) field;
		List<SFXBase> listFields = fieldComposite.getComposition();

		String scNodeCode = nodeCode.getTextContent();
		Node nodeParent = nodeCode.getParentNode();
		
		for (int iIndex = 1, iTotal = listFields.size(); iIndex<iTotal; ++iIndex)
		{
			// Deep copy the node structure
			Node nodeDuplicate = nodeParent.cloneNode(true);
			
			// Rename the code content
			renameBelow(nodeDuplicate, scNodeCode, Integer.toString(iIndex));
			
			// Process the code nodes
			extractFields(nodeDuplicate, dataModel);
			
			// Add the tree into the profile document after the duplicated node {no insertBefore so use insertAfter.next}
			Node nodeBase = nodeParent.getParentNode();
			nodeBase.insertBefore(nodeDuplicate, nodeParent.getNextSibling());
		}
		
		// Now rename the original nodes so all other nodes in the same branches point to the same child
		renameBelow(nodeParent, scNodeCode, "0");

		// Continue with mapping of nodeCode to the first child
		return setWrapper(nodeCode, dataModel, listFields.get(0), scField, zIsHeader);
	}
	
	private SFXTextBase renameBelow(Node nodeRoot, String scReplace, String scWith)
	{
		// Find all the nodes from the root with the name that starts <something>.* and replace with <something>.scWith
		String[] ascContent = scReplace.split("[.*.]", 2);
		String scXPATH = String.format("//code[starts-with(.,'%s.*')]", ascContent[0]);
		
		try 
		{
			// Use XPATH to find <code> nodes
			XPathFactory xPathfactory = XPathFactory.newInstance();
			XPath xpath = xPathfactory.newXPath();
			// Create an xpath to find all the code nodes with the content <something>.*
			XPathExpression xpathExpression = xpath.compile(scXPATH);
			
			NodeList listCode = (NodeList) xpathExpression.evaluate(nodeRoot, XPathConstants.NODESET);
			
			for (int iLength = listCode.getLength(), iIndex = 0; iIndex<iLength; ++iIndex)
			{
				// Treat each code node			
				Node nodeCode = listCode.item(iIndex);
				
				String scOriginal = nodeCode.getTextContent();
				String[] ascOriginal = scOriginal.split("[.*.]", 2);
				String scNew = String.format("%s.%s.%s", ascContent[0], scWith, ascOriginal[1].substring(2));				
				
				nodeCode.setTextContent(scNew);
			}			
		} 
		catch (XPathExpressionException x) 
		{
			throw new RuntimeException(x);
		}
		
		return null;
	}
	
	/** 
	 * Create a wrapper or wrapper link if field already exists 
	 * and add to the map of fields against wrappers 
	 */
	
	private SFXTextBase mapWrapper(SFXBase field, String scAccess, boolean zIsHeader)
	{
		SFXTextBase textWrapper = null;
		
		if (m_mapWrappers.containsKey(field))
		{			
			SFXTextBase textExisting = m_mapWrappers.get(field);
			// This links the new field so that wrapper replaces existing with link to original
			SFXTextLink textLinked = new SFXTextLink();
			textWrapper = textLinked.linkField(textExisting);
		}
		else
		{
			textWrapper = new SFXTextBase();
		}

		textWrapper = textWrapper.setField(field).setCheck(zIsHeader).setAccess(m_queryView, scAccess);		
		m_mapWrappers.put(field, textWrapper);
		
		return textWrapper;
	}
	
	protected boolean suppressNode(SFXTextBase textWrapper)
	{
		List<Node> listNodes = new ArrayList<Node>();
		
		// Remove all the nodes that use the wrapper
		for (Map.Entry<Node, SFXTextBase> entry : m_mapSource.entrySet())
		{
			if (entry.getValue()!=textWrapper) continue;
			// Remove the node from the document
			Node node = entry.getKey();
			Node parent = node.getParentNode();
			parent.removeChild(node);
			listNodes.add(node);
		}
		
		// Remove all the nodes with the wrapper from the source map
		for (Node node : listNodes)
		{
			m_mapSource.remove(node);
		}
		
		return true;
	}
		
	/* ======================================================================
	   Copy the HTML document outline to a new document dropping those 
	   sections that are not relevant and substituting values
	   ====================================================================== */
	
	//private int m_iLevel = 0;
	
	protected boolean shallowCopy(Node nodeParent, Node nodeSource)
	{
		// Only shallow copy the node type
		Document document = nodeParent.getOwnerDocument();
		if (null==document) document = (Document) nodeParent;
		Node nodeCopy = document.importNode(nodeSource, false); // false - shallow copy
		
		//System.out.println(String.format("Level %d copy <%s> [%s]", m_iLevel++, nodeCopy.getNodeName(), nodeCopy.getNodeValue()));
		nodeParent.appendChild(nodeCopy);
		
		// Copy the children
		return deepCopy(nodeCopy, nodeSource, 0, false);
	}
	
	protected boolean deepCopy(Node nodeTarget, Node nodeParent, int iFrom, boolean zSkipRemainder) 
	{
		NodeList listChildren = nodeParent.getChildNodes();
		
		for (int iIndex = iFrom, iTotal = listChildren.getLength(); iIndex<iTotal; ++iIndex)
		{
			Node nodeSource = listChildren.item(iIndex);
			SFXTextBase textWrapper = m_mapSource.get(nodeSource);
			
			if (null!=textWrapper) return conditionalCopy(nodeTarget, nodeParent, iIndex, textWrapper);
			if (shallowCopy(nodeTarget, nodeSource)) return dropCopy(nodeTarget, zSkipRemainder);
		}
		
		//System.out.println(String.format("Level %d deep </%s>", --m_iLevel, nodeTarget.getNodeName()));

		return zSkipRemainder;
	}
	
	protected boolean conditionalCopy(Node nodeTarget, Node nodeParent, int iFrom, SFXTextBase textWrapper)
	{
		Document document = nodeTarget.getOwnerDocument();
		// Add a string with the name of the underlying field
		
		Node nodeSpan = document.createElement("span");
		String scField = textWrapper.m_fieldContent.m_scField;
		String scAccess = textWrapper.toString();
		String scValue = String.format("'%s.%s' ", scField, scAccess);
		Node nodeCopy = document.createTextNode(scValue);		
		nodeSpan.appendChild(nodeCopy);
		
		nodeTarget.appendChild(nodeSpan);
		
		return deepCopy(nodeTarget, nodeParent, iFrom+1, false);
	}
	
	private boolean dropCopy(Node nodeTarget, boolean zSkipRemainder)
	{
		//if (!zSkipRemainder) System.out.println(String.format("Level %d drop </%s>", --m_iLevel, nodeTarget.getNodeName()));
		return zSkipRemainder;
	}
}

public class SFXTextDescription extends SFXTextOutline implements SFXModelListener
{
	private static final long serialVersionUID = 1143808594660148056L;

	private Document m_documentPopulated = null;
	private Timer m_timer = null;
	
	
	@Override
	public SFXBase createField(String scField)
	{
		m_timer = new Timer();
		
		return super.createField(scField);
	}

	@Override
	public String getValue()
	{
		return transformDocument(m_documentPopulated.getDocumentElement());
	}
	
	public SFXTextOutline insertChangePassing(SFXModelListener listener, SFXModel dataModel)
	{
		// When ready to insert into listening chain set the listener for the update of this document
		m_listFieldListeners.add(listener);
		// ... and listen to the source data for changes
		dataModel.addListener(this);
		
		return this;
	}
	
	/* ======================================================================
	   Model listener
	   ====================================================================== */
		
	@Override
	public boolean changedField(SFXBase field) 
	{
		// Ignore fields that are not mapped
		if (!m_mapWrappers.containsKey(field)) return false;
			
		m_timer.cancel();
		m_timer = new Timer();
		
		TimerTask task = new TimerTask()
		{
			@Override
			public void run() 
			{
				updateDocument();
				return;
			}
		};
		
		// Schedule a check in 200ms
		m_timer.schedule(task, 200);
		
		return true;
	}
	
	private boolean updateDocument()
	{
		// Possibly one of the fields has changed
		for (Map.Entry<SFXBase, SFXTextBase> entryWrapper : m_mapWrappers.entrySet())
			if (entryWrapper.getValue().hasUpdate()) return prepareDocument(entryWrapper);
		// If none of the fields have changed then nothing to do
		return false;
	}

	// Create document from original outline by copying parts that wrappers identify as valid
	private boolean prepareDocument(Map.Entry<SFXBase, SFXTextBase> entryWrapper)
	{
		Iterator<Entry<SFXBase, SFXTextBase>> iterateList = m_mapWrappers.entrySet().iterator();
		
		// Skip over those fields already checked
		while (iterateList.hasNext())
			if (iterateList.next()==entryWrapper) break;
		
		// Update remaining fields
		while (iterateList.hasNext())
			iterateList.next().getValue().hasUpdate();
		
		// Copy the source document exchanging the code nodes for the field values and update listeners
		m_documentPopulated = m_builderPopulated.newDocument();		
		shallowCopy(m_documentPopulated, m_documentOutline.getDocumentElement());
		
		// Tell listeners field has changed and when they call getValue changes document to value
		changedField();
		
		return true;
	}
	
	@Override
	public boolean addField(final SFXBase fieldAdded) 
	{
		m_timer.cancel();
		m_timer = new Timer();
		
		TimerTask task = new TimerTask()
		{
			@Override
			public void run() 
			{
				reloadDocument();
				return;
			}
		};

		// Schedule a check in 2000ms
		m_timer.schedule(task, 2000);			
		
		return true;
	}
	
	private boolean reloadDocument()
	{
		SFXTextDescription descriptionNew = new SFXTextDescription();
		descriptionNew.createField(m_scField);
		descriptionNew.loadOutline(m_queryView, m_dataModel, m_scFile);
		
		m_documentOutline = descriptionNew.m_documentOutline;
		m_mapSource = descriptionNew.m_mapSource;
		m_mapWrappers = descriptionNew.m_mapWrappers;
		
		return updateDocument();
	}

	@Override
	public boolean replaceField(SFXBase fieldReplace, SFXBase fieldWith) 
	{
		SFXTextBase textWrapper = m_mapWrappers.get(fieldReplace);
		if (null==textWrapper) return false;
		// Need to update wrapper
		m_mapWrappers.remove(fieldReplace);
		textWrapper.setField(fieldWith);
		m_mapWrappers.put(fieldWith, textWrapper);
		return true;
	}

	@Override
	public boolean removeField(SFXBase fieldRemoved) 
	{
		// Ignore fields that are not mapped
		if (!m_mapWrappers.containsKey(fieldRemoved)) return false;
		
		// Field was removed but has content in the document
		SFXTextBase textWrapper = m_mapWrappers.remove(fieldRemoved);
		if (suppressNode(textWrapper)) return true;
		
		throw new RuntimeException(String.format("Cannot remove %s since used for description", fieldRemoved.toString()));
	}
	
	@Override
	protected boolean conditionalCopy(Node nodeTarget, Node nodeParent, int iFrom, SFXTextBase textWrapper)
	{
		// If the field is a substitution then copy the value in place and then remainder of the nodes
		if (textWrapper.useValue()) return valueCopy(nodeTarget, nodeParent, iFrom+1, textWrapper);
		// If the field is included then simply continue copy node without the <code/> node
		if (textWrapper.includeContent()) return deepCopy(nodeTarget, nodeParent, iFrom+1, false);

		// Remove the target node too because it will not include anything
		Node nodeAbove = nodeTarget.getParentNode();
		nodeAbove.removeChild(nodeTarget);
		
		//System.out.println(String.format("Level %d skip [%s]", --m_iLevel, nodeTarget.getNodeName()));
		// Find the parent node name to use for comparison to next data want to include
		String scParentType = nodeParent.getNodeName(); // This should be the hN name
		// Otherwise need to find the next node of the same type of the parent-parent
		Node nodeHolder = nodeParent.getParentNode(); // This should be the parent of the <hN> node with the conditional check
		
		NodeList listChildren = nodeHolder.getChildNodes();
		int iIndex = 0;
		int iTotal = listChildren.getLength();
		
		for (; iIndex<iTotal; ++iIndex)
		{
			Node nodeSource = listChildren.item(iIndex);
			// First find conditional node
			if (nodeSource==nodeParent) break;
		}
		
		for (++iIndex; iIndex<iTotal; ++iIndex)
		{
			Node nodeSource = listChildren.item(iIndex);
			// Find next node same level same type
			String scSourceType = nodeSource.getNodeName();
			
			if (scSourceType.equals(scParentType)) return deepCopy(nodeAbove, nodeHolder, iIndex, true);
		}
		
		return true;
	}
	
	private boolean valueCopy(Node nodeTarget, Node nodeParent, int iFrom, SFXTextBase textWrapper)
	{
		String scValue = textWrapper.getValue();
		//System.out.println(String.format("Level %d text [%s]", m_iLevel, scValue));
		
		// Create a text node in substitution for the conditional node
		Node nodeCopy = m_documentPopulated.createTextNode(scValue);
		nodeTarget.appendChild(nodeCopy);
		
		// Continue with copy of remainder
		return deepCopy(nodeTarget, nodeParent, iFrom, false);
	}
}