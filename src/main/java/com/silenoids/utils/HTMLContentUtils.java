package com.silenoids.utils;

import static j2html.TagCreator.body;
import static j2html.TagCreator.br;
import static j2html.TagCreator.code;
import static j2html.TagCreator.div;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.h2;
import static j2html.TagCreator.h3;
import static j2html.TagCreator.img;
import static j2html.TagCreator.p;

public class HTMLContentUtils {
    public static String getHelpContent() {
        return body(
                h1("Helping you out!"),
                    h2("cioè aspe!"),
                        h3("ma quindi!"),
                        code("questo è un codice"),
                        br(),
                        p("wewe bello facimme"),
                        div(),
                        img().withSrc(getImgSrc("donateBtn.png"))
        ).render();
    }

    private static String getImgSrc(String imgPath) {
        return HTMLContentUtils.class.getClassLoader().getResource(imgPath).toString();
    }

}
