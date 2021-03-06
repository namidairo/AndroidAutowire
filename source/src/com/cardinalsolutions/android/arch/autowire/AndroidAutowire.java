package com.cardinalsolutions.android.arch.autowire;

import java.lang.reflect.Field;

import android.app.Activity;
import android.view.View;

/**
 * Annotation handler class that will wire in the android views at runtime.
 * <br /><br />
 * This class will look for the {@code @AndroidView} annotation in the activity class.
 * <br /><br />
 * <strong>Example Usage:</strong>
 * <br /><br />
 * Base Class for Activity
 * <pre class="prettyprint">
 * public class BaseActivity extends Activity {
 * 	...
 * 	{@code @Override}
 *	public void setContentView(int layoutResID){
 *		super.setContentView(layoutResID);
 *		AndroidAutowire.autowire(this, BaseActivity.class);
 *	}
 * }
 * </pre>
 * Activity Class
 * <pre class="prettyprint">
 * public class MainActivity extends BaseActivity{
 * 	{@code @AndroidView}
 * 	private Button main_button;
 * 
 * 	{@code @AndroidView(id="edit_text_field")}
 * 	private EditText editText;
 * 
 * 	{@code @AndroidView(resId=R.id.img_logo, required=false)}
 * 	private ImageView logo;
 * 
 * 	{@code @Override}
 *	protected void onCreate(Bundle savedInstanceState) {		
 *		super.onCreate(savedInstanceState);
 *		setContentView(R.layout.activity_main)
 * 	}
 * }
 * </pre>
 * The layout xml :
 * <pre class="prettyprint">
 *  
 * &lt;EditText
 *    android:id="@+id/edit_text_field"
 *    android:layout_width="fill_parent" 
 *    android:layout_height="wrap_content"
 *    android:inputType="textUri" 
 *    /&gt;
 *  
 * &lt;Button  
 *    android:id="@+id/main_button"
 *    android:layout_width="fill_parent" 
 *    android:layout_height="wrap_content" 
 *    android:text="@string/test"
 *    /&gt;
 *    
 * &lt;ImageView  
 *    android:id="@+id/img_logo"
 *    android:layout_width="fill_parent" 
 *    android:layout_height="wrap_content" 
 *    android:text="@string/hello"
 *   /&gt;
 * </pre>
 * @author Jacob Kanipe-Illig (jkanipe-illig@cardinalsolutions.com)
 * Copyright (c) 2013
 */
public class AndroidAutowire {

	/**
	 * Perform the wiring of the Android View using the {@link AndroidView} annotation.
	 * <br /><br />
	 * <strong>Usage:</strong><br /><br />
	 * Annotation all view fields in the activity to be autowired.  Use {@code @AndroidView}.<br />
	 * If you do not specify the {@code id} or the {@code resId} parameters in the annotation, the name of the variable will be used as the id.
	 * <br />
	 * You may specify whether or not the field is required (true by default).
	 * <br /><br />
	 * After the call to {@code setContentView(layoutResID)} in the onCreate() method, you will call this 
	 * {@code autowire(Activity thisClass, Class<?> baseClass)} method.
	 * <br />
	 * The first parameter is the Activity class being loaded.  <br />
	 * The second parameter is the class of the base activity (if applicable).
	 *
	 * @param thisClass The activity being created.
	 * @param baseClass The Base activity. If there is inheritance in the activities, this is the highest level, the base activity,
	 * but not {@link Activity}. <br /><br />All views annotated with {@code @AndroidView} will be autowired in all Activity classes in the 
	 * inheritance structure, from thisClass to baseClass inclusive. baseClass should not be {@link Activity} because no fields
	 * in {@link Activity} will need to be autowired. <br /><br /> If there is no parent class for your activity, use thisClass.class as baseClass.
	 * @throws AndroidAutowireException Indicates that there was an issue autowiring a view to an annotated field. Will not be thrown if required=false
	 * on the {@link AndroidView} annotation.
	 */
	public static void autowire(Activity thisClass, Class<?> baseClass) throws AndroidAutowireException{
		Class<?> clazz = thisClass.getClass();
		autowireViewsForClass(thisClass, clazz);
		//Do this for all classes in the inheritance chain, until we get to this class
		while(baseClass.isAssignableFrom(clazz.getSuperclass())){
			clazz = clazz.getSuperclass();
			autowireViewsForClass(thisClass, clazz);
		}
	}
	
	private static void autowireViewsForClass(Activity thisActivity, Class<?> clazz){
		for (Field field : clazz.getDeclaredFields()){
			if(!field.isAnnotationPresent(AndroidView.class)){
				continue;
			}
			if(!View.class.isAssignableFrom(field.getType())){
				continue;
			}
			AndroidView androidView = field.getAnnotation(AndroidView.class);
			int resId = androidView.resId();
			if(resId == 0){
				String viewId = androidView.id();
				if(androidView.id().equals("")){
					viewId = field.getName();
				}
				resId = thisActivity.getResources().getIdentifier(viewId, "id", thisActivity.getPackageName());			
			}
			try {
				View view = thisActivity.findViewById(resId);
				if(view == null){
					if(!androidView.required()){
						continue;
					}else{
						throw new AndroidAutowireException("No view resource with the id of " + resId + " found. "
								+" The required field " + field.getName() + " could not be autowired" );	
					}
				}
				field.setAccessible(true);
				field.set(thisActivity,view);
			} catch (Exception e){
				throw new AndroidAutowireException("Cound not Autowire AndroidView: " + field.getName() + ". " + e.getMessage());
			}
		}
	}
}
