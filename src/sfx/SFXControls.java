package sfx;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;

public class SFXControls implements SFXViewListener
{
	private SFXView m_view = null;
	private JFileChooser m_chooseFile = null;
	
	private boolean m_zDirty = false;

	public SFXControls createControls(SFXView view)
	{
		m_view = view;
		m_chooseFile = new JFileChooser();
		FileFilter filterSFX = new SFXFilter();
		m_chooseFile.addChoosableFileFilter(filterSFX);
		m_chooseFile.setFileFilter(filterSFX);
		view.addListener(this);
		return this;
	}
	
	@Override
	public boolean duplicateField(SFXBase field) 
	{
		try 
		{
			SFXComposite composite = (SFXComposite) field.getParent();
			Class<? extends SFXBase> classField = field.getClass();
			SFXBase instanceField = (SFXBase)classField.newInstance();
			// Internal composition responsibility of parent
			composite.addField(instanceField);
		} 
		catch (InstantiationException x) 
		{
			x.printStackTrace();
			return false;
		} 
		catch (IllegalAccessException x) 
		{
			x.printStackTrace();
			return false;
		}
		m_zDirty = true;
		
		return true;
	}
	
	@Override
	public boolean modifyValue(int iValue, SFXBase field) 
	{
		// The field can decide how to change the value
		field.setValue(iValue);
		m_zDirty = true;
		return true;
	}

	@Override
	public boolean modifyValue(String scValue, SFXBase field) 
	{
		// The field can decide how to change the value
		field.setValue(scValue);
		m_zDirty = true;
		return true;
	}
	
	@Override
	public boolean modifyMultiplier(int iMultiplier, SFXBase field) 
	{
		SFXValue value = (SFXValue) field;
		value.setMultiplierIndex(iMultiplier);
		m_zDirty = true;
		return true;
	}
	
	@Override
	public boolean removeField(SFXBase field) 
	{
		SFXComposite composite = (SFXComposite) field.getParent();
		composite.removeField(field);
		m_zDirty = true;
		return true;
	}
	
	@Override
	public boolean actionFile(String actionCommand, SFXModel dataModel) 
	{
		if (actionCommand.equals("New")) return newModel(dataModel);
		if (actionCommand.equals("Open")) return loadModel(dataModel);
		if (actionCommand.equals("Save")) return saveModel(dataModel);
		if (actionCommand.equals("Exit")) return closeModel(dataModel);
		if (actionCommand.equals("Print")) return printModel(dataModel);
		if (actionCommand.equals("Push")) return pushModel(dataModel);
		if (actionCommand.equals("Pull")) return pullModel(dataModel);
		if (actionCommand.equals("Drop")) return dropModel(dataModel);
		return true;		
	}
	
	private boolean newModel(SFXModel dataModel) 
	{
		if (maintainDirty()) return false;
		SFXModel newModel = new SFXModel();
		newModel.createModel();
		dataModel.replaceContent(newModel);
		// Replacing the model includes adjustment to number of composite fields children so sets dirty
		m_zDirty = false;
		return true;
	}
	
	private boolean loadModel(SFXModel dataModel) 
	{
		if (maintainDirty()) return false;
		
		int iOption = m_chooseFile.showOpenDialog(null);
		if (JFileChooser.APPROVE_OPTION!=iOption) return false;
		File file = m_chooseFile.getSelectedFile();
		
		ObjectInputStream objectLoadStream = null;
		return loadModel(file, objectLoadStream, dataModel);		
	}

	private boolean saveModel(SFXModel dataModel) 
	{
		int iOption = m_chooseFile.showSaveDialog(null);
		if (JFileChooser.APPROVE_OPTION!=iOption) return false;
		File file = m_chooseFile.getSelectedFile();
		
		ObjectOutputStream objectSaveStream = null;
		return saveModel(file, objectSaveStream, dataModel);		
	}
	
	private boolean closeModel(SFXModel dataModel) 
	{
		if (maintainDirty()) return false;
		System.exit(0);
		return false;
	}

	private boolean printModel(SFXModel dataModel) 
	{
		final JComponent panelDescription = m_view.currentTab(dataModel);
		
		PrinterJob job = PrinterJob.getPrinterJob();
		job.setJobName(dataModel.getField("Name").getValue());
		if (!job.printDialog()) return false;
		
		Printable context = new Printable()
		{
			@Override
			public int print(Graphics graphics, PageFormat formatPage, int iPage) throws PrinterException 
			{
				// Restrict print to single page
				if (0<iPage) return Printable.NO_SUCH_PAGE;
				
				Graphics2D graphics2D = (Graphics2D) graphics;
				graphics2D.translate(formatPage.getImageableX(), formatPage.getImageableY());
				
				double dfWidth = formatPage.getImageableWidth() - formatPage.getImageableX();
				double dfHeight = formatPage.getImageableHeight() - formatPage.getImageableY();
				Dimension dimPaper = new Dimension();
				dimPaper.setSize(dfWidth, dfHeight);
				panelDescription.setPreferredSize(dimPaper);
				
				panelDescription.print(graphics2D);
				return Printable.PAGE_EXISTS;
			}
		};
		
		try 
		{
			job.setPrintable(context);
			job.print();
		} 
		catch (PrinterException x) 
		{
			x.printStackTrace();
		}
		
		return true;
	}
	
	private boolean pushModel(SFXModel dataModel) 
	{
		try 
		{
			// Create a model correctly to make sure non-serialized fields are available
			SFXModel dataReturn = new SFXModel();
			dataReturn.createModel();
			
			// Take a copy of the data model using serialization
			ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();
			ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteOutputStream);
			objectOutputStream.writeObject(dataModel);
 
	        //De-serialization of object
	        ByteArrayInputStream byteInputStream = new   ByteArrayInputStream(byteOutputStream.toByteArray());
	        ObjectInputStream objectInputStream = new ObjectInputStream(byteInputStream);
	        SFXModel dataPush = (SFXModel) objectInputStream.readObject();
	        
			// Create a new tab
	        m_view.createTab(dataReturn);
	        
	        // Take the serialized fields from the cloned class - updates the view
	        dataReturn.replaceContent(dataPush);	        
		} 
		catch (IOException x) 
		{
			x.printStackTrace();
		} 
		catch (ClassNotFoundException x) 
		{
			x.printStackTrace();
		}
		
		return true;
	}
	
	private boolean pullModel(SFXModel dataModel) 
	{
		if (maintainDirty()) return false;
		
		// Find selected model data
		SFXModel dataPull = m_view.removeTab();
		if (null==dataPull) return false;
		
		// Pull back from stored data
		dataModel.replaceContent(dataPull);
		
		return true;
	}

	private boolean dropModel(SFXModel dataModel) 
	{
		// Find selected model data
		m_view.removeTab();
		return true;
	}
	
	private boolean loadModel(File file, ObjectInputStream objectLoadStream, SFXModel dataModel) 
	{
		try 
		{
			FileInputStream fileLoadStream = new FileInputStream(file);
			BufferedInputStream bufferLoadStream = new BufferedInputStream(fileLoadStream);
			
			objectLoadStream = new ObjectInputStream(bufferLoadStream);
			SFXModel loadModel = (SFXModel)objectLoadStream.readObject();
			
			dataModel.replaceContent(loadModel);
			//Replacing the model includes adjustment to number of composite fields children so sets dirty
			m_zDirty = false;
		} 
		catch (FileNotFoundException x) 
		{
			x.printStackTrace();
		} 
		catch (IOException x) 
		{
			x.printStackTrace();
		} 
		catch (ClassNotFoundException x) 
		{
			x.printStackTrace();
		}
		
		try 
		{
			if (null!=objectLoadStream) objectLoadStream.close();
		} 
		catch (IOException x) 
		{
			x.printStackTrace();
		}
		
		return false;
	}

	private boolean saveModel(File file, ObjectOutputStream objectSaveStream, SFXModel dataModel)
	{
		try 
		{
			FileOutputStream fileSaveStream = new FileOutputStream(file);
			BufferedOutputStream bufferSaveStream = new BufferedOutputStream(fileSaveStream);
			
			objectSaveStream = new ObjectOutputStream(bufferSaveStream);
			objectSaveStream.writeObject(dataModel);
		} 
		catch (FileNotFoundException x) 
		{
			x.printStackTrace();
		} 
		catch (IOException x) 
		{
			x.printStackTrace();
		}
		
		try 
		{
			if (null!=objectSaveStream) objectSaveStream.close();
		} 
		catch (IOException x) 
		{
			x.printStackTrace();
		}
		
		m_zDirty = false;
		
		return true;
	}
	
	private boolean maintainDirty()
	{
		if (!m_zDirty) return false;
		
		Object[] options = {"Do NOT discard current settings", "Discard current settings"};
		int iOption = JOptionPane.showOptionDialog(null, "The settings of the current design have been changed", 
				"Settings have been changed!", JOptionPane.YES_NO_OPTION, 
				JOptionPane.WARNING_MESSAGE, null, options, options[0]);
		
		if (0==iOption) return true;
		
		m_zDirty = false;		
		return false;
	}
}

class SFXFilter extends FileFilter
{
	@Override
	public boolean accept(File fileCheck) 
	{
		if (fileCheck.isDirectory()) return true;
		String scFile = fileCheck.getName().toLowerCase();
		if (scFile.endsWith("sfx")) return true;
		return false;
	}

	@Override
	public String getDescription() 
	{
		return "Just SFX files";
	}
}
