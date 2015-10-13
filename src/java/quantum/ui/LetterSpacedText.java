// SOURCE: http://stackoverflow.com/questions/30220679/how-to-set-the-letter-spacing-aka-tracking-of-a-font-in-javafx

package quantum.ui;

import javafx.scene.Node;
import javafx.scene.layout.FlowPane;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.paint.Color;

public class LetterSpacedText extends FlowPane {
    private Font font;
    private Color fill;

    public LetterSpacedText(String s, double spacing) {
        setText(s);
        setHgap(spacing);
    }

    public void setText(String s) {
        getChildren().clear();
        for (int i = 0; i < s.length(); i++) {
            getChildren().add(new Text("" + s.charAt(i)));
        }
        setFont(this.font);
        setFill(this.fill);
    }

    public void setFont(Font font) {
        if (font != null) {
            this.font = font;
            for (Node t : getChildren()) {
                ((Text) t).setFont(font);
            }
        }
    }

    public void setFill(Color fill) {
        if(fill != null) {
            this.fill = fill;
            for (Node t : getChildren()) {
                ((Text) t).setFill(fill);
            }
        }
    }
}