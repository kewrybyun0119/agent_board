package com.example.board;

import com.example.board.entity.Post;
import com.example.board.repository.PostRepository;
import com.example.board.service.PostService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:wordreplacetest;DB_CLOSE_DELAY=-1")
class WordReplaceTransactionTest {

    @Autowired
    PostService service;

    @Autowired
    PostRepository repository;

    @BeforeEach
    void clean() {
        repository.deleteAll();
    }

    private Post save(String title, String content) {
        Post p = new Post();
        p.setTitle(title);
        p.setName("kim");
        p.setContent(content);
        return repository.save(p);
    }

    @Test
    void 정상_치환은_모든_글에_반영된다() {
        Post a = save("kfc 치킨", "kfc 먹자");
        Post b = save("hello", "kfc kfc");

        int changed = service.replaceWord(List.of(a.getId(), b.getId()), "kfc", "bhc");

        assertThat(changed).isEqualTo(2);
        assertThat(repository.findById(a.getId()).orElseThrow().getTitle()).isEqualTo("bhc 치킨");
        assertThat(repository.findById(b.getId()).orElseThrow().getContent()).isEqualTo("bhc bhc");
    }

    @Test
    void 중간_글에서_실패하면_앞선_글도_롤백된다() {
        Post ok = save("kfc", "kfc");
        // 제목 100자(상한). 치환하면 102자가 되어 검증에 실패한다.
        Post tooLong = save("kfc" + "x".repeat(97), "내용");

        assertThatThrownBy(() -> service.replaceWord(List.of(ok.getId(), tooLong.getId()), "kfc", "bhc!!"))
                .isInstanceOf(Exception.class);

        Post reloaded = repository.findById(ok.getId()).orElseThrow();
        assertThat(reloaded.getTitle()).as("앞선 글이 일부만 바뀌면 안 된다").isEqualTo("kfc");
        assertThat(reloaded.getContent()).isEqualTo("kfc");
    }
}
