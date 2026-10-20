package ru.practicum.main.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import ru.practicum.main.dto.request.AdminEventSearchFilter;
import ru.practicum.main.dto.request.PublicEventSearchFilter;
import ru.practicum.main.model.Event;
import ru.practicum.main.model.QEvent;
import ru.practicum.main.model.QParticipationRequest;
import ru.practicum.main.model.enums.ParticipationStatus;

@Repository
@RequiredArgsConstructor
public class EventSearchRepository {

    private final JPAQueryFactory queryFactory;

    public Page<Event> searchAdmin(AdminEventSearchFilter filter, Pageable pageable) {
        QEvent e = QEvent.event;
        BooleanBuilder where = buildAdminPredicate(filter);

        java.util.List<Event> content = queryFactory.selectFrom(e)
                .leftJoin(e.initiator).fetchJoin()
                .leftJoin(e.category).fetchJoin()
                .where(where)
                .orderBy(e.eventDate.asc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory.select(e.count())
                .from(e)
                .where(where)
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    public Page<Event> searchPublic(PublicEventSearchFilter filter, Pageable pageable) {
        QEvent e = QEvent.event;
        BooleanBuilder where = buildPublicPredicate(filter);

        java.util.List<Event> content = queryFactory.selectFrom(e)
                .leftJoin(e.initiator).fetchJoin()
                .leftJoin(e.category).fetchJoin()
                .where(where)
                .orderBy(e.eventDate.asc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory.select(e.count())
                .from(e)
                .where(where)
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    private BooleanBuilder buildAdminPredicate(AdminEventSearchFilter filter) {
        QEvent e = QEvent.event;
        BooleanBuilder where = new BooleanBuilder();

        if (filter.users() != null) {
            where.and(e.initiator.id.in(filter.users()));
        }
        if (filter.states() != null) {
            where.and(e.state.in(filter.states()));
        }
        if (filter.categories() != null) {
            where.and(e.category.id.in(filter.categories()));
        }
        if (filter.rangeStart() != null) {
            where.and(e.eventDate.goe(filter.rangeStart()));
        }
        if (filter.rangeEnd() != null) {
            where.and(e.eventDate.loe(filter.rangeEnd()));
        }

        return where;
    }

    private BooleanBuilder buildPublicPredicate(PublicEventSearchFilter filter) {
        QEvent e = QEvent.event;
        QParticipationRequest r = QParticipationRequest.participationRequest;
        BooleanBuilder where = new BooleanBuilder();

        if (filter.state() != null) {
            where.and(e.state.eq(filter.state()));
        }
        if (filter.text() != null) {
            where.and(e.annotation.containsIgnoreCase(filter.text())
                    .or(e.description.containsIgnoreCase(filter.text())));
        }
        if (filter.categories() != null) {
            where.and(e.category.id.in(filter.categories()));
        }
        if (filter.paid() != null) {
            where.and(e.paid.eq(filter.paid()));
        }
        if (filter.rangeStart() != null) {
            where.and(e.eventDate.goe(filter.rangeStart()));
        }
        if (filter.rangeEnd() != null) {
            where.and(e.eventDate.loe(filter.rangeEnd()));
        }

        if (Boolean.TRUE.equals(filter.onlyAvailable())) {
            where.and(e.participantLimit.eq(0)
                    .or(e.participantLimit.gt(
                            com.querydsl.jpa.JPAExpressions.select(r.count())
                                    .from(r)
                                    .where(r.event.eq(e)
                                            .and(r.status.eq(ParticipationStatus.CONFIRMED)))
                    )));
        }

        return where;
    }
}
