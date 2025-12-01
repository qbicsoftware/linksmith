package life.qbic.linksmith.internal.parsing;

import java.util.List;

public record RawLink(String rawURI, List<RawParam> rawParameters) {

}
