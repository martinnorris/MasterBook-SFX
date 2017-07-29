package sfx;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;

interface SFXViewListener 
{
	public boolean duplicateField(SFXBase field);
	public boolean modifyValue(int iValue, SFXBase field);
	public boolean modifyValue(String scValue, SFXBase field);
	public boolean modifyMultiplier(int iMultiplier, SFXBase field);
	public boolean removeField(SFXBase field);
	public boolean actionFile(String actionCommand, SFXModel dataModel);
}

interface SFXViewQuery
{
	public SFXViewField getView(SFXField field);
}

public class SFXView implements SFXModelListener, SFXViewQuery
{
	// The model
	private SFXModel m_dataModel = null;
	// Map the model to the content
	private Map<SFXField, SFXViewField> m_mapDataContent = null;
	
	// List of controls that listen for changes shared with all the elements that are built by the view
	private List<SFXViewListener> m_listControls = null;
	private JPanel m_panelContent = null;
	private JMenuBar m_menuBar = null;
	
	// To create duplicate tabs keep map of tab created and data contained
	private JPanel m_panelDescription = null;
	private JTextField m_textName = null;
	private Map<JPanel, SFXModel> m_mapDataPanel = null;
	
	/* ======================================================================
	   Content
	   ====================================================================== */
	
	public SFXView createView(SFXModel dataModel) 
	{
		m_dataModel = dataModel;
		
		// Mapping of model to view
		m_mapDataContent = new HashMap<SFXField, SFXViewField>();
		m_listControls = new CopyOnWriteArrayList<SFXViewListener>();
		// Add as a listener so that model has sink for events fired when first request values
		dataModel.addListener(this);
		
		JPanel panelName = createName(new JPanel(), dataModel);
		
		// Panel for the basics
		JPanel panelMandatory = createMandatory(new JPanel(), dataModel);
		
		JPanel panelOptional = createOptional(new JPanel(), dataModel);
		JScrollPane paneOptional = new JScrollPane(panelOptional);
		
		JPanel panelModifiers = createModifiers(new JPanel(), dataModel);
		JScrollPane paneModifiers = new JScrollPane(panelModifiers);
		
		JPanel panelResult = createResult(new JPanel(), dataModel);
		
		JPanel panelSpecific = createSpecific(new JPanel(), dataModel);
		
		JPanel panelDescription = createDescription(new JPanel(), dataModel);
		
		// Add the panels into a set of tabs
		JTabbedPane pane = new JTabbedPane();
		pane.addTab("Mandatory", panelMandatory);
		pane.addTab("Optional", paneOptional);
		pane.addTab("Modifiers", paneModifiers);
		pane.addTab("Specific", panelSpecific);
		pane.addTab("Description", panelDescription);
		
		m_panelContent = new JPanel();
		m_panelContent.setLayout(new BorderLayout());
		m_panelContent.add(panelName, BorderLayout.NORTH);
		m_panelContent.add(pane, BorderLayout.CENTER);
		m_panelContent.add(panelResult, BorderLayout.SOUTH);
		
		m_menuBar = createMenu(new JMenuBar(), dataModel);
		m_mapDataPanel = new HashMap<JPanel, SFXModel>();
		
		return this;
	}
	
	private JPanel createName(JPanel panelName, SFXModel dataModel)
	{
		panelName.setLayout(new FlowLayout());
		
		SFXSwingSub subName = new SFXSwingSub()
		{
			private static final long serialVersionUID = -7841190977037651687L;

			@Override
			protected SFXViewField populatePanel(String scLabel, List<SFXViewListener> listListeners) 
			{
				setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
				
				m_textValue = new SFXSwingText();
				m_textValue.setColumns(SFXSwingText._LINE);
				m_textValue.setEditable(true);
				m_textValue.addActionListener(this);
				add(m_textValue);				
				return this;
			}	
			
			@Override
			protected SFXViewField modifyText(SFXSwingControl control, String scCommand)
			{
				String scName = control.getText();
				// Remove whitespace
				scName = scName.replaceAll("\\s+", "");
				int iLength = 12<scName.length()?12:scName.length();
				scName = scName.substring(0, iLength);
				scName = scName.concat(" 1");
				m_textName.setText(scName);
				return super.modifyText(control, scCommand);
			}
		};
		subName.createPanel("Name of the Special Effect", m_listControls);
		subName.setValue(viewModel(dataModel.getField("Name"), subName));
		panelName.add(subName);
		
		return panelName;
	}
	
	private JPanel createMandatory(JPanel panelBasic, SFXModel dataModel)
	{
		panelBasic.setLayout(new BoxLayout(panelBasic, BoxLayout.PAGE_AXIS));
		
		SFXSwingScalar panelEffect = new SFXSwingScalar();
		panelEffect.createPanel("Effect", m_listControls);
		panelEffect.setValue(viewModel(dataModel.getField("Effect"), panelEffect));
		panelBasic.add(panelEffect);
		
		SFXSwingValue panelRange = new SFXSwingValue();
		panelRange.createPanel("Range", m_listControls);
		panelRange.setValue(viewModel(dataModel.getField("Range"), panelRange));
		panelBasic.add(panelRange);
		
		SFXSwingValueDefault panelSpeed = new SFXSwingValueDefault();
		panelSpeed.createPanel("Speed", m_listControls);
		panelSpeed.setValue(viewModel(dataModel.getField("Speed"), panelSpeed));
		panelBasic.add(panelSpeed);
		
		SFXSwingValue panelDuration = new SFXSwingValue();
		panelDuration.createPanel("Duration", m_listControls);
		panelDuration.setValue(viewModel(dataModel.getField("Duration"), panelDuration));
		panelBasic.add(panelDuration);
		
		SFXSwingSub panelSub = new SFXSwingSub();
		panelSub.createPanel("SFX subtotal", m_listControls);
		panelSub.setValue(viewModel(dataModel.getField("SubMandatory"), panelSub));
		panelBasic.add(panelSub);
		
		SFXSwingSub panelHalf = new SFXSwingSub();
		panelHalf.createPanel("SFX minimum", m_listControls);
		panelHalf.setValue(viewModel(dataModel.getField("HalfMandatory"), panelHalf));
		panelBasic.add(panelHalf);
		
		SFXSwingValue panelCasting = new SFXSwingValue();
		panelCasting.createPanel("Casting time", m_listControls);
		panelCasting.setValue(viewModel(dataModel.getField("Casting"), panelCasting));
		panelBasic.add(panelCasting);
		
		panelBasic.add(Box.createVerticalGlue());
		
		return panelBasic;
	}

	private JPanel createOptional(JPanel panelOptional, SFXModel dataModel)
	{
		panelOptional.setLayout(new BoxLayout(panelOptional, BoxLayout.PAGE_AXIS));
		
		SFXCompositeArea compositeArea = (SFXCompositeArea)dataModel.getField("AreaEffect");
		
		SFXSwingParent panelStep = new SFXSwingParentArea();
		panelStep.createPanel("Area effects", m_listControls);
		panelStep.setValue(viewModel(compositeArea, panelStep));
		
		for (SFXBase field : compositeArea.getComposition())
		{
			SFXViewField panel = panelStep.createContent(field);
			viewModel(field, panel);
			panelStep.add(panel.getPanel());
		}
		
		panelOptional.add(panelStep);
		panelOptional.add(Box.createVerticalStrut(8));
		
		SFXSwingScalar panelMultiT = new SFXSwingScalar();
		panelMultiT.createPanel("Multiple targets", m_listControls);
		panelMultiT.setValue(viewModel(dataModel.getField("MultiTarget"), panelMultiT));
		panelOptional.add(panelMultiT);
		
		SFXSwingChoice panelMultiA = new SFXSwingChoice();
		panelMultiA.createPanel("Multiple attributes", m_listControls);
		panelMultiA.setValue(viewModel(dataModel.getField("MultiAttribute"), panelMultiA));
		panelOptional.add(panelMultiA);
		
		SFXSwingScalar panelChange = new SFXSwingScalar();
		panelChange.createPanel("Change targets", m_listControls);
		panelChange.setValue(viewModel(dataModel.getField("ChangeTarget"), panelChange));
		panelOptional.add(panelChange);
		
		SFXSwingScalar panelVariableEffect = new SFXSwingScalar();
		panelVariableEffect.createPanel("Variable effect", m_listControls);
		panelVariableEffect.setValue(viewModel(dataModel.getField("VariableEffect"), panelVariableEffect));
		panelOptional.add(panelVariableEffect);
		
		SFXComposite compositeDuration = (SFXComposite)dataModel.getField("VariableDuration");

		SFXSwingParent panelDuration = new SFXSwingParentDuration();
		panelDuration.createPanel("Variable duration", m_listControls);
		panelDuration.setValue(viewModel(compositeDuration, panelDuration));
		
		for (SFXBase field : compositeDuration.getComposition())
		{
			SFXViewField panel = panelDuration.createContent(field);
			viewModel(field, panel);
			panelDuration.add(panel.getPanel());
		}
		
		panelOptional.add(panelDuration);
		panelOptional.add(Box.createVerticalStrut(8));

		SFXComposite compositeApportion = (SFXComposite)dataModel.getField("Apportation");
		
		SFXSwingParent panelApportation = new SFXSwingParentApportation();
		panelApportation.createPanel("Apportation", m_listControls);
		panelApportation.setValue(viewModel(compositeApportion, panelApportation));		
		
		for (SFXBase field : compositeApportion.getComposition())
		{
			SFXViewField panel = panelApportation.createContent(field);
			viewModel(field, panel);
			panelApportation.add(panel.getPanel());
		}
		
		panelOptional.add(panelApportation);
		panelOptional.add(Box.createVerticalStrut(8));

		SFXSwingEnable panelMaintenance = new SFXSwingEnable();
		panelMaintenance.createPanel("Maintenance", m_listControls);
		panelMaintenance.setValue(viewModel(dataModel.getField("Maintenance"), panelMaintenance));
		panelOptional.add(panelMaintenance);
		
		SFXSwingEnable panelFocus = new SFXSwingEnable();
		panelFocus.createPanel("Focus", m_listControls);
		panelFocus.setValue(viewModel(dataModel.getField("Focus"), panelFocus));
		panelOptional.add(panelFocus);
		
		SFXSwingCharges panelCharges = new SFXSwingCharges();
		panelCharges.createPanel("Charges", m_listControls);
		panelCharges.setValue(viewModel(dataModel.getField("Charges"), panelCharges));
		panelOptional.add(panelCharges);
		
		panelOptional.add(Box.createVerticalGlue());
		
		return panelOptional;
	}

	private JPanel createModifiers(JPanel panelModifiers, SFXModel dataModel)
	{
		panelModifiers.setLayout(new BoxLayout(panelModifiers, BoxLayout.PAGE_AXIS));
		
		SFXComposite compositeCommunity = (SFXComposite)dataModel.getField("Community");
		
		SFXSwingParent panelStep = new SFXSwingParent();
		panelStep.setChild(SFXSwingCommunity.class).createPanel("Community", m_listControls);
		panelStep.setValue(viewModel(compositeCommunity, panelStep));
		
		for (SFXBase field : compositeCommunity.getComposition())
		{
			SFXSwingCommunity panelCommunity = new SFXSwingCommunity();
			panelCommunity.createPanel("Community", m_listControls);
			panelCommunity.setValue(viewModel(field, panelCommunity));
			panelStep.add(panelCommunity);			
		}
			
		panelModifiers.add(panelStep);
		panelModifiers.add(Box.createVerticalStrut(8));
		
		SFXComposite compositeComponents = (SFXComposite)dataModel.getField("Components");
		
		SFXSwingParent panelComponents = new SFXSwingParent();
		panelComponents.setChild(SFXSwingComponent.class).createPanel("Components", m_listControls);
		panelComponents.setValue(viewModel(compositeComponents, panelComponents));
		
		for (SFXBase field : compositeComponents.getComposition())
		{
			SFXSwingComponent panelComponent = new SFXSwingComponent();
			panelComponent.createPanel("Component", m_listControls);
			panelComponent.setValue(viewModel(field, panelComponent));
			panelComponents.add(panelComponent);			
		}
			
		panelModifiers.add(panelComponents);
		panelModifiers.add(Box.createVerticalStrut(8));
		
		SFXSwingConcentration panelConcentration = new SFXSwingConcentration();
		panelConcentration.createPanel("Concentration", m_listControls);
		panelConcentration.setValue(viewModel(dataModel.getField("Concentration"), panelConcentration));
		panelModifiers.add(panelConcentration);
		
		SFXComposite compositeGestures = (SFXComposite)dataModel.getField("Gestures");
		
		SFXSwingParent panelGestures = new SFXSwingParent();
		panelGestures.setChild(SFXSwingGesture.class).createPanel("Gestures", m_listControls);
		panelGestures.setValue(viewModel(compositeGestures, panelGestures));
		
		for (SFXBase field : compositeGestures.getComposition())
		{
			SFXSwingGesture panelComponent = new SFXSwingGesture();
			panelComponent.createPanel("Gesture", m_listControls);
			panelComponent.setValue(viewModel(field, panelComponent));
			panelGestures.add(panelComponent);			
		}
			
		panelModifiers.add(panelGestures);
		panelModifiers.add(Box.createVerticalStrut(8));
		
		SFXSwingIncantation panelIncantation = new SFXSwingIncantation();
		panelIncantation.createPanel("Incantation", m_listControls);
		panelIncantation.setValue(viewModel(dataModel.getField("Incantation"), panelIncantation));
		panelModifiers.add(panelIncantation);
		
		SFXSwingRelatedSkill panelRelatedskill = new SFXSwingRelatedSkill();
		panelRelatedskill.createPanel("Related skill", m_listControls);
		panelRelatedskill.setValue(viewModel(dataModel.getField("RelatedSkill"), panelRelatedskill));
		panelModifiers.add(panelRelatedskill);
		
		SFXSwingOthers panelOther = new SFXSwingOthers();
		panelOther.createPanel("Other", m_listControls);
		panelOther.setValue(viewModel(dataModel.getField("Other"), panelOther));
		panelModifiers.add(panelOther);
		
		SFXSwingUnreal panelUnreal = new SFXSwingUnreal();
		panelUnreal.createPanel("Unreal", m_listControls);
		panelUnreal.setValue(viewModel(dataModel.getField("Unreal"), panelUnreal));
		panelModifiers.add(panelUnreal);
		
		panelModifiers.add(Box.createVerticalGlue());
		
		return panelModifiers;
	}
	
	private JPanel createResult(JPanel panelBottom, SFXModel dataModel)
	{
		JPanel panelResult = new JPanel();
		panelResult.setLayout(new BoxLayout(panelResult, BoxLayout.PAGE_AXIS));
		
		SFXSwingSub panelMandatory = new SFXSwingSub();
		panelMandatory.createPanel("SFX mandatory", m_listControls);
		panelMandatory.setValue(viewModel(dataModel.getField("Mandatory"), panelMandatory));
		panelResult.add(panelMandatory);
		
		SFXSwingSub panelOptional = new SFXSwingSub();
		panelOptional.createPanel("SFX optional", m_listControls);
		panelOptional.setValue(viewModel(dataModel.getField("Optional"), panelOptional));
		panelResult.add(panelOptional);
		
		SFXSwingProportion panelSlider = new SFXSwingProportion();
		panelSlider.createPanel("Divide DN and FV", m_listControls);
		panelSlider.setValue(viewModel(dataModel.getField("Fraction"), panelSlider));
		panelResult.add(panelSlider);
		
		SFXSwingModifiers panelModifiers = new SFXSwingModifiers();
		panelModifiers.createPanel("SFX modifiers", m_listControls);
		panelModifiers.setValue(viewModel(dataModel.getField("Modifiers"), panelModifiers));
		panelResult.add(panelModifiers);		

		SFXSwingDouble panelTotal = new SFXSwingDouble();
		panelTotal.createPanel("SFX totals", m_listControls);
		panelTotal.setValue(viewModel(dataModel.getField("Total"), panelTotal));
		panelResult.add(panelTotal);
		
		JPanel panelButtons = createPushPull(new JPanel());
		
		Dimension dimButtons = panelButtons.getPreferredSize();
		JPanel panelPadding = new JPanel();
		panelPadding.setPreferredSize(dimButtons);
		
		panelBottom.setLayout(new BoxLayout(panelBottom, BoxLayout.LINE_AXIS));
		panelBottom.add(Box.createHorizontalStrut(12));
		panelBottom.add(panelPadding);
		panelBottom.add(panelResult);
		panelBottom.add(panelButtons);
		panelBottom.add(Box.createHorizontalStrut(12));
		
		return panelBottom;
	}
	
	private JPanel createPushPull(JPanel panelPushPull)
	{
		// Pass the button actions through to the listeners
		
		ActionListener listenerButtons = new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent eventAction) 
			{
				String scCommand = eventAction.getActionCommand();
				
				for (SFXViewListener listener : m_listControls)
					listener.actionFile(scCommand, m_dataModel);
				return;
			}			
		};
		
		panelPushPull.setLayout(new BoxLayout(panelPushPull, BoxLayout.PAGE_AXIS));
		
		panelPushPull.add(Box.createVerticalStrut(8));
		
		JButton buttonMove = new JButton("Move to tab");
		buttonMove.setAlignmentX(Container.CENTER_ALIGNMENT);
		buttonMove.setActionCommand("Push");
		buttonMove.addActionListener(listenerButtons);
		panelPushPull.add(buttonMove);		
		panelPushPull.add(Box.createVerticalStrut(8));
		
		JButton buttonReturn = new JButton("Return to settings");
		buttonReturn.setAlignmentX(Container.CENTER_ALIGNMENT);
		buttonReturn.setActionCommand("Pull");
		buttonReturn.addActionListener(listenerButtons);
		panelPushPull.add(buttonReturn);
		panelPushPull.add(Box.createVerticalStrut(8));
		
		m_textName = new JTextField("Move");
		m_textName.setColumns(SFXSwingText._COST);
		
		FlowLayout layout = new FlowLayout();
		layout.setAlignment(FlowLayout.CENTER);
		
		JPanel panelText = new JPanel();
		panelText.setLayout(layout);
		panelText.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
		panelText.add(m_textName);
		
		panelPushPull.add(panelText);
		panelPushPull.add(Box.createVerticalStrut(8));
		
		JButton buttonRemove = new JButton("Remove tab");
		buttonRemove.setAlignmentX(Container.CENTER_ALIGNMENT);
		buttonRemove.setActionCommand("Drop");
		buttonRemove.addActionListener(listenerButtons);
		panelPushPull.add(buttonRemove);
		panelPushPull.add(Box.createVerticalStrut(12));
		
		panelPushPull.add(Box.createVerticalGlue());
		
		return panelPushPull;
	}
	
	private JPanel createSpecific(JPanel panelSpecific, SFXModel dataModel)
	{
		panelSpecific.setLayout(new BoxLayout(panelSpecific, BoxLayout.PAGE_AXIS));

		SFXSwingSkill panelSkill = new SFXSwingSkill();
		panelSkill.createPanel("Skill", m_listControls);
		panelSkill.setValue(viewModel(dataModel.getField("Skill"), panelSkill));
		panelSpecific.add(panelSkill);
		
		SFXSwingEnable panelConcentrationAdd = new SFXSwingEnable();
		panelConcentrationAdd.createPanel("Isolated concentration (reduction)", m_listControls);
		panelConcentrationAdd.setValue(viewModel(dataModel.getField("ConcentrationAdd"), panelConcentrationAdd));
		panelSpecific.add(panelConcentrationAdd);
		
		SFXSwingChoice panelReception = new SFXSwingChoice();
		panelReception.createPanel("Reception (reduction)", m_listControls);
		panelReception.setValue(viewModel(dataModel.getField("Reception"), panelReception));
		panelSpecific.add(panelReception);
		
		SFXSwingChoice panelTrance = new SFXSwingChoice();
		panelTrance.createPanel("Trance (reduction)", m_listControls);
		panelTrance.setValue(viewModel(dataModel.getField("Trance"), panelTrance));
		panelSpecific.add(panelTrance);
		
		SFXSwingChoice panelLock = new SFXSwingChoice();
		panelLock.createPanel("Lock (cost)", m_listControls);
		panelLock.setValue(viewModel(dataModel.getField("Lock"), panelLock));
		panelSpecific.add(panelLock);
		
		SFXSwingChoice panelCountenance = new SFXSwingChoice();
		panelCountenance.createPanel("Countenance (reduction)", m_listControls);
		panelCountenance.setValue(viewModel(dataModel.getField("Countenance"), panelCountenance));
		panelSpecific.add(panelCountenance);
		
		SFXBase fieldSpecific = dataModel.getField("Specific");
		
		SFXSwingSub panelSubTotal = new SFXSwingSub();
		panelSubTotal.createPanel("SFX sub total", m_listControls).setValue(fieldSpecific);
		panelSpecific.add(panelSubTotal);
		
		SFXSwingDouble panelTotal = new SFXSwingDouble();
		panelTotal.createPanel("SFX totals", m_listControls).setValue(fieldSpecific);
		panelSpecific.add(panelTotal);
		
		SFXSwingTimeFV panelTimeTotal = new SFXSwingTimeFV();
		panelTimeTotal.createPanel("SFX FV as time", m_listControls).setValue(fieldSpecific);
		panelSpecific.add(panelTimeTotal);
		
		SFXSwingContainer parentTotal = new SFXSwingContainer();
		parentTotal.createPanel("SFX total container", m_listControls);
		parentTotal.setChild(panelSubTotal).setChild(panelTotal).setChild(panelTimeTotal);
		viewModel(fieldSpecific, parentTotal);
		
		return panelSpecific;
	}
	
	private JPanel createDescription(JPanel panel, SFXModel dataModel)
	{
		SFXSwingTextPane panelDescription = new SFXSwingTextPane();
		panelDescription.createPanel("Description", m_listControls);
		SFXBase descriptionData = panelDescription.createContent(dataModel, this, this);
		viewModel(descriptionData, panelDescription);

		// Can immediately return panel created for text for pane content
		m_panelDescription = panelDescription;
		
		return panelDescription;
	}
	
	private JMenuBar createMenu(JMenuBar menuBar, SFXModel dataModel)
	{
		// Pass the menu actions through to the listeners
		
		ActionListener listenerMenu = new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent eventAction) 
			{
				String scCommand = eventAction.getActionCommand();
				if (handleMenu(scCommand)) return;
				
				for (SFXViewListener listener : m_listControls)
					listener.actionFile(scCommand, m_dataModel);
				return;
			}			
		};
						
		JMenuItem itemNew = new JMenuItem("New");
		itemNew.setActionCommand("New");
		itemNew.addActionListener(listenerMenu);
		
		JMenuItem itemOpen = new JMenuItem("Open");
		itemOpen.setActionCommand("Open");
		itemOpen.addActionListener(listenerMenu);

		JMenuItem itemSave = new JMenuItem("Save");
		itemSave.setActionCommand("Save");
		itemSave.addActionListener(listenerMenu);
		
		JMenuItem itemPrint = new JMenuItem("Print");
		itemPrint.setActionCommand("Print");
		itemPrint.addActionListener(listenerMenu);
		
		JMenuItem itemExit = new JMenuItem("Exit");
		itemExit.setActionCommand("Exit");
		itemExit.addActionListener(listenerMenu);
		
		JMenu menuFile = new JMenu("File");
		menuFile.add(itemNew);
		menuFile.add(itemOpen);
		menuFile.add(itemSave);
		menuFile.addSeparator();
		menuFile.add(itemPrint);
		menuFile.addSeparator();
		menuFile.add(itemExit);
		
		menuBar.add(menuFile);
		
		JMenuItem itemHelp = new JMenuItem("Help");
		itemHelp.setActionCommand("Help");
		itemHelp.addActionListener(listenerMenu);
		
		JMenuItem itemAbout = new JMenuItem("About");
		itemAbout.setActionCommand("About");
		itemAbout.addActionListener(listenerMenu);
		
		JMenuItem itemOutline = new JMenuItem("Outline");
		itemOutline.setActionCommand("Outline");
		itemOutline.addActionListener(listenerMenu);
		
		JMenu menuHelp = new JMenu("Help");
		menuHelp.add(itemHelp);
		menuHelp.add(itemAbout);
		menuHelp.add(itemOutline);
		
		menuBar.add(menuHelp);
		
		return menuBar;
	}
	
	/* ======================================================================
	   Framing
	   ====================================================================== */
	
	private static Runnable createFrame(final Component componentContainer, final String scFrameTitle, final JMenuBar barMenu, final int iStart, final int iExit)
    {
    	final JFrame frame = new JFrame(scFrameTitle);
    	
    	return new Runnable()
    	{
			@Override
			public void run() 
			{
				// Need to make Swing play nice with light weight over heavy weight components
				JPopupMenu.setDefaultLightWeightPopupEnabled(false);
				
				frame.setDefaultCloseOperation(iExit);
				frame.setJMenuBar(barMenu);
				frame.add(componentContainer);
				
				frame.pack();
				
				if (Frame.MAXIMIZED_BOTH==iStart)
					frame.setExtendedState(Frame.MAXIMIZED_BOTH);
				else
					frame.setLocation(new Point(80, 100)); // set position if not maximised
				
				frame.setVisible(true);
			}
    	};
    }
	
	private static JPanel frameView(String scTitle, JPanel panelContent, JMenuBar menuBar, int iExit)
	{
		Runnable startLater = createFrame(panelContent, scTitle, menuBar, Frame.NORMAL, iExit);
		
    	if (SwingUtilities.isEventDispatchThread())
			startLater.run();
		else
			SwingUtilities.invokeLater(startLater);
    	
    	return panelContent;
	}
	
	public SFXView frameView() 
	{
		m_panelContent.setPreferredSize(new Dimension(800, 600));
		frameView("MasterBook SFX Calculation", m_panelContent, m_menuBar, JFrame.EXIT_ON_CLOSE);
		return this;
	}

	/* ======================================================================
	   Listen for data changes
	   ====================================================================== */
		
	private SFXBase viewModel(SFXBase fieldModel, SFXViewField fieldView) 
	{
		m_mapDataContent.put(fieldModel, fieldView);
		return fieldModel;
	}

	@Override
	public SFXViewField getView(SFXField field)
	{
		return m_mapDataContent.get(field);
	}
	
	@Override
	public boolean addField(final SFXBase field) 
	{
		SFXComposite fieldParent = (SFXComposite)field.getParent();
		SFXViewField swingParent = getView(fieldParent);
		if (null==swingParent) return false;
		
		SFXViewField swingCreated = swingParent.createContent(field);
		viewModel(field, swingCreated);
		
		final JComponent panelParent = swingParent.getPanel();
		final JComponent panelCreated = swingCreated.getPanel();

		Runnable changeField = new Runnable()
		{
			@Override
			public void run() 
			{
				panelParent.add(panelCreated);
				panelParent.revalidate();
			}
		};
		
    	try 
    	{
    		if (SwingUtilities.isEventDispatchThread())
    			changeField.run();
    		else
    			SwingUtilities.invokeAndWait(changeField);
		} 
    	catch (InvocationTargetException x) 
    	{
			x.printStackTrace();
		}
    	catch (InterruptedException x) 
    	{
			x.printStackTrace();
		}
    			
		return true;
	}
	
	@Override
	public boolean changedField(final SFXBase field) 
	{
		final SFXViewField fieldValue = getView(field);
		if (null==fieldValue) 
			return false; // Need check because data can change behind making changes before fields are prepared
		
		Runnable changeField = new Runnable()
		{
			@Override
			public void run() 
			{
				fieldValue.setValue(field);
			}
		};
		
    	if (SwingUtilities.isEventDispatchThread())
			changeField.run();
		else
			SwingUtilities.invokeLater(changeField);
    	
		return true;
	}
	
	@Override
	public boolean replaceField(SFXBase fieldReplace, SFXBase fieldWith) 
	{
		SFXViewField fieldValue = m_mapDataContent.remove(fieldReplace);
		m_mapDataContent.put(fieldWith, fieldValue);
		return changedField(fieldWith);
	}

	@Override
	public boolean removeField(SFXBase fieldRemoved) 
	{
		final SFXViewField fieldValue = m_mapDataContent.remove(fieldRemoved);
		if (null==fieldValue) return false;
		
		Runnable removeField = new Runnable()
		{
			@Override
			public void run() 
			{
				JComponent content = fieldValue.getPanel();
				Container container = content.getParent();
				container.remove(content);
				container.revalidate();
			}
		};
		
    	try 
    	{
    		if (SwingUtilities.isEventDispatchThread())
    			removeField.run();
    		else
    			SwingUtilities.invokeAndWait(removeField);
		} 
    	catch (InvocationTargetException x) 
    	{
			x.printStackTrace();
		}
    	catch (InterruptedException x) 
    	{
			x.printStackTrace();
		}
    	
		return true;
	}
	
	/* ======================================================================
	   Listeners for controls - notification done by content who share listeners
	   ====================================================================== */
	
	public void addListener(SFXViewListener listener)
	{
		m_listControls.add(listener);
	}
	
	public void removeListener(SFXViewListener listener)
	{
		m_listControls.remove(listener);
	}
	
	/* ======================================================================
	   Menu items that are handled by the view
	   ====================================================================== */
	
	private boolean handleMenu(String scCommand)
	{
		if (scCommand.startsWith("About")) return showAbout();
		if (scCommand.startsWith("Help")) return showAbout();
		if (scCommand.startsWith("Outline")) return showOutline();
		return false;
	}
	
	private boolean showAbout()
	{
		SFXViewHTMLAbout panelAbout = new SFXViewHTMLAbout();
		frameView("Masterbook SFX", panelAbout.createPanel(this, m_dataModel), null, JFrame.DISPOSE_ON_CLOSE);
		return true;
	}
	
	private boolean showOutline()
	{
		SFXViewHTMLOutline panelOutline = new SFXViewHTMLOutline();
		frameView("Masterbook SFX", panelOutline.createPanel(this, m_dataModel), null, JFrame.DISPOSE_ON_CLOSE);
		return true;
	}
	
	private JTabbedPane getTabbedPane()
	{
		Component[] acomponents = m_panelContent.getComponents();
		JTabbedPane pane = null;
		
		for (int iIndex = 0; iIndex<acomponents.length; ++iIndex)
		{
			if (acomponents[iIndex] instanceof JTabbedPane) 
			{
				pane = (JTabbedPane) acomponents[iIndex];
				break;
			}
		}
		
		return pane;
	}
	
	public JComponent currentTab(SFXModel dataModel)
	{
		// Find the selected tab pane
		JTabbedPane pane = getTabbedPane();
		JPanel panelSelected = (JPanel) pane.getSelectedComponent();
		
		// Check if want to print selected pane
		SFXModel dataPull = m_mapDataPanel.get(panelSelected);
		if (null==dataPull) panelSelected = m_panelDescription;

		// Extract the JPanel from the panel with the scroll pane content
		JScrollPane paneTab = (JScrollPane) panelSelected.getComponent(0);
		JViewport portTab = (JViewport) paneTab.getComponent(0);
		JTextPane paneText = (JTextPane) portTab.getComponent(0);	
		
		return paneText;
	}
	
	public JPanel createTab(SFXModel dataModel)
	{
		// Create new tab and populate with field view (already existing)
		JPanel panelDescription = createDescription(new JPanel(), dataModel);
		
		String scName = m_textName.getText();
		if (0>=scName.length()) scName = "Moved";
		Pattern pattern = Pattern.compile("\\d+$");
		Matcher matcher = pattern.matcher(scName);
		
		if (matcher.find())
		{
			String scNumber = matcher.group(0);
			String scFront = scName.substring(0, scName.length() - scNumber.length());
			int i = Integer.parseInt(scNumber);
			String scNext = String.format("%s %d", scFront, ++i);
			m_textName.setText(scNext);
		}
		else
		{
			String scNext = scName.concat(" 1");
			m_textName.setText(scNext);			
		}
		
		JTabbedPane pane = getTabbedPane();
		pane.addTab(scName, panelDescription);
		
		m_mapDataPanel.put(panelDescription, dataModel);
		
		return panelDescription;
	}
	
	public SFXModel removeTab()
	{
		// Find the selected tab pane
		JTabbedPane pane = getTabbedPane();
		JPanel panelSelected = (JPanel) pane.getSelectedComponent();
		
		SFXModel dataPull = m_mapDataPanel.get(panelSelected);
		if (null==dataPull) return null;
		
		// Do not listen to model embedded
		dataPull.removeListener(this);
		m_mapDataPanel.remove(panelSelected);
		
		Container container = panelSelected.getParent();
		container.remove(panelSelected);
		
		return dataPull;
	}
	
}

class SFXViewHTMLPanel extends JPanel implements ActionListener
{
	private static final long serialVersionUID = -7730479934178524430L;

	public SFXViewHTMLPanel createPanel(SFXViewQuery queryView, SFXModel dataModel)
	{
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		
		JTextPane pane = new JTextPane();
		pane.setEditable(false);
		pane.setContentType("text/html");
		pane.setText("<html><body bgcolor=#fffaec><p>Loading HTML ...</p></body></html>");
		
		pane = getContent(queryView, pane, dataModel);
		
        JScrollPane paneScroll = new JScrollPane(pane);
        paneScroll.setPreferredSize(new Dimension(700, 400));
        
        JButton buttonOK = new JButton();
        buttonOK.setText("OK");
        buttonOK.setAlignmentX(Component.CENTER_ALIGNMENT);        
        buttonOK.addActionListener(this);
        
        add(paneScroll);
        add(Box.createVerticalStrut(8));
        add(buttonOK);
        add(Box.createVerticalStrut(12));
        
		return this;
	}
	
	protected JTextPane getContent(SFXViewQuery queryView, JTextPane pane, SFXModel dataModel)
	{
		pane.setText("HTML appears here");
		return pane;
	}
	
	@Override
	public void actionPerformed(ActionEvent event)
	{
		Window windowSource = SwingUtilities.getWindowAncestor(this);
		WindowEvent eventClosing = new WindowEvent(windowSource, WindowEvent.WINDOW_CLOSING);
		
		windowSource.dispatchEvent(eventClosing);
		windowSource.setVisible(false);
		windowSource.dispose();
		
		return;
	}
}

class SFXViewHTMLAbout extends SFXViewHTMLPanel
{
	private static final long serialVersionUID = 6449816805033079707L;

	@Override
	protected JTextPane getContent(SFXViewQuery queryView, JTextPane pane, SFXModel dataModel)
	{
        try
        {
        	// Get the HTML content of a stream {using stream so can load from jar file}
        	InputStream stream = getClass().getClassLoader().getResourceAsStream("resources/About.html");
        	pane.read(stream, "About HTML file");
        }
        catch (IOException x)
        {
        	pane.setText("... could not find pretty 'About' file, just to let you know");
			x.printStackTrace();
        } 
		
		return pane;
	}	
}

class SFXViewHTMLOutline extends SFXViewHTMLPanel
{
	private static final long serialVersionUID = 805399284536118742L;

	@Override
	protected JTextPane getContent(SFXViewQuery queryView, JTextPane pane, SFXModel dataModel)
	{
		SFXTextOutline descriptionData = new SFXTextOutline();
		descriptionData.createField("Outline");		
		descriptionData.loadOutline(queryView, dataModel, "resources/Outline.html"); //.insertChangePassing(listener, dataModel);

		String scContent = descriptionData.getValue();
		// Remove lots of whitespace
		scContent = scContent.replaceAll("\\r\\n", " ");
		pane.setText(scContent);
		
		return pane;
	}	
}

