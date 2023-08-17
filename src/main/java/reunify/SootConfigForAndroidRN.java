package reunify;

import soot.jimple.infoflow.android.config.SootConfigForAndroid;
import soot.jimple.infoflow.InfoflowConfiguration;
import soot.options.Options;


public class SootConfigForAndroidRN extends SootConfigForAndroid{
    private String hbcTool;

    public SootConfigForAndroidRN(String hbcTool) {
        super();
        this.hbcTool = hbcTool;

    }

    @Override
	public void setSootOptions(Options options, InfoflowConfiguration config) {
        super.setSootOptions(options, config);
        options.set_react_native(true);
        options.set_hbc_nativehost_path(hbcTool);
    }
    
}
