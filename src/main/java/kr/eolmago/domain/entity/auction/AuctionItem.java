package kr.eolmago.domain.entity.auction;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import kr.eolmago.domain.entity.auction.enums.ItemCategory;
import kr.eolmago.domain.entity.auction.enums.ItemCondition;
import kr.eolmago.domain.entity.common.AuditableEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "auction_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuctionItem extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false, updatable = false)
    private Long auctionItemId;

    @Column(nullable = false, length = 100)
    private String itemName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ItemCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ItemCondition condition;

    // 카테고리별 추가 스펙을 담는 JSONB 컬럼
    // {"brand": "Apple", "storageGb": 256}
    @Type(JsonType.class)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> specs;

    public static AuctionItem create(
            String itemName,
            ItemCategory category,
            ItemCondition condition,
            Map<String, Object> specs
    ) {
        AuctionItem item = new AuctionItem();
        item.itemName = itemName;
        item.category = category;
        item.condition = condition;

        if (specs != null) {
            item.specs = new HashMap<>(specs);
        }

        return item;
    }

    // 임시저장 수정
    public void updateDraft(String itemName, ItemCategory category, ItemCondition condition, Map<String, Object> specs) {
        this.itemName = itemName;
        this.category = category;
        this.condition = condition;
        this.specs = specs;
    }
}