package sfx;

public class SFXApp 
{

	public static void main(String[] args) 
	{
		SFXModel dataModel = new SFXModel();
		dataModel.createModel();
		
		SFXView viewSwing = new SFXView();
		viewSwing.createView(dataModel).frameView();
		
		SFXControls controls = new SFXControls();
		controls.createControls(viewSwing);
	}

}
