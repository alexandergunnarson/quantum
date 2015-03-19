(ns quantum.ui.custom-ui)

; Create the control
(def-fx my-control my-custom-control)
(conj-fx! rt.my-control);

; Create the behavior

public class MyCustomControlBehavior extends BehaviorBase {
   public MyCustomControlBehavior(MyCustomControl control) {
      super(control);
   }
}

; Create the Skin
;The BaseSkin was a private API; it was changed to public with Java 8.
public class MyCustomControlSkin extends SkinBase{
   public MyCustomControlSkin(MyCustomControl control) {
      super(control, new MyCustomControlBehavior(control));
   }
}

; Create the Control
public class MyCustomControl extends Control {
   public MyCustomControl() {
   }
}

;The problem is that the control class do not know anything about the skin or behavior.
;This was one of the biggest pitfalls I was confronted with while learning JavaFX.

;With JavaFX it should be very easy to create different visualisation/skins
;for controls.
;You can customize the look of components by css.
;The skin is the main part of this look; it has to defined by css, too.
;Instead of creating a skin object for the control by your own you only
;define the skin class that should be used for your control. The instanciation
;and everything else is automatically done by the JavaFX APIs. To do so you
;have to bind your control to a css class. First off all you have to create a
;new css file in your project. I think best practice is to use the same
;package as the controls has and but a css file under src/main/resource:
;Inside the css you have to specify a new selector for your component and add
;the skin as a property to it. This will for example look like this:

.custom-control {
   -fx-skin: "com.guigarage.customcontrol.MyCustomControlSkin";
}

;Once you have created the css you have to define it in your control. Therefore you have to configure the path to the css file and the selector of your component:
;public class MyCustomControl extends Control {
    
   public MyCustomControl() {
      getStyleClass().add("custom-control");
   }
 
   @Override
   protected String getUserAgentStylesheet() {
      return MyCustomControl.class.getResource("customcontrol.css").toExternalForm();
   }
}

;After all this stuff is done correctly JavaFX will create a skin instance for your control. You do not need to take care about this instantiation or the dependency mechanism. At this point I want to thank Jonathan Giles who taked some time to code the css integration for gridfx together with me and explained me all the mechanisms and benefits.

;Normally there is no need to access the skin or the behavior from within the controller. But if you have the need to do you can access them this way:

;Because controler.getSkin() receives a javafx.scene.control.Skin and not a SkinBase you have to cast it if you need the Behavior:
;((SkinBase)getSkin()).getBehavior();

;Workaround for css haters
For some of you this mechanism seems to be a little to oversized. Maybe you only need a specific control once in your application and you do not plan to skin it with css and doing all this stuff. For this use case there is a nice workaround in the JavaFX API. You can ignore all the css stuff and set the skin class to your control in code:
public class MyCustomControl extends Control {
   public MyCustomControl() {
      setSkinClassName(MyCustomControlSkin.class.getName());
   }
}

The benefit of this workflow is that refactoring of packages or classnames don’t break your code and you don’t need a extra css file. On the other side there is a great handicap. You can’t use css defined skins in any extension of this control. I think that every public API like gridfx should use the css way. In some internal use cases the hard coded way could be faster.

Conclusion
Now we created a control, a skin and a behavior that are working fine and can be added to your UI tree. But like in swing when simply extending the JComponent you don’t see anything on screen. So the next step is to style and layout your component. I will handle this topic in my next post.
If you want to look at some code of existing custom components check out jgridfx or JFXtras. At jgridfx the following files match with this article:

com.guigarage.fx.grid.GridView (control)
com.guigarage.fx.grid.skin.GridViewSkin (skin)
com.guigarage.fx.grid.behavior.GridViewBehavior (behavior)
/src/main/resources/com/guigarage/fx/grid/gridview.css (css)
