package ru.erked.bl.systems;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;

public class BLTextSystem {

    private static String text;

    public BLTextSystem(int lang){
        switch (lang){
            case 0: {
                FileHandle textFile = Gdx.files.internal("lang/EN.txt");
                text = textFile.readString();
                break;
            }
            case 1: {
                FileHandle textFile = Gdx.files.internal("lang/RU.txt");
                text = textFile.readString();
                break;
            }
            default:{
                text = "null";
            }
        }
    }

    public String get(String key){
        String result = "null";
        for(int i=0; i < text.length() - key.length(); i++){
            if(text.substring(i, i + key.length()).equals(key)){
                int j = i + key.length() + 1;
                while(!text.substring(j, j+1).equals("☺")){
                    j++;
                }
                result = text.substring(i + key.length() + 1, j);
            }
        }
        return result;
    }

}
