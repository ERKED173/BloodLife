package ru.erked.bl.systems;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;

import ru.erked.bl.MainBL;

public class BLButton {

    private TextButton button;

    public BLButton(MainBL game, float x, float y, float width, BitmapFont font, String text, int id, String name){
        float param;
        Skin skin = new Skin();
        skin.addRegions(game.atlas);
        TextButton.TextButtonStyle textButtonStyle;
        textButtonStyle = new TextButton.TextButtonStyle();
        textButtonStyle.font = font;
        switch (id){
            case 1:{
                textButtonStyle.up = skin.getDrawable("button_up");
                textButtonStyle.down = skin.getDrawable("button_down");
                textButtonStyle.checked = skin.getDrawable("button_checked");
                param = 0.5f;
                break;
            }
            case 2:{
                textButtonStyle.up = skin.getDrawable("square_button_up");
                textButtonStyle.down = skin.getDrawable("square_button_down");
                textButtonStyle.checked = skin.getDrawable("square_button_checked");
                param = 1.0f;
                break;
            }
            default:{
                param = 1.0f;
                break;
            }
        }

        button = new TextButton(text, textButtonStyle);
        button.setSize(width, param * width);
        button.setPosition(x, y);
        button.setName(name);
    }

    public TextButton get(){
        return button;
    }

}
