package kr.eolmago.domain.entity.auction.enums;

public enum ItemCategory {
    PHONE("휴대폰"),
    TABLET("태블릿");

    private final String label;

    ItemCategory(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}