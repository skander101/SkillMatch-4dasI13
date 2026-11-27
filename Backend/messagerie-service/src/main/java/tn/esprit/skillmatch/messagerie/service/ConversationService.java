package tn.esprit.skillmatch.messagerie.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import tn.esprit.skillmatch.messagerie.config.AuthenticatedUser;
import tn.esprit.skillmatch.messagerie.config.UserClient;
import tn.esprit.skillmatch.messagerie.dto.AdminConversationDetailResponse;
import tn.esprit.skillmatch.messagerie.dto.AdminConversationSummaryResponse;
import tn.esprit.skillmatch.messagerie.dto.AuditEntryResponse;
import tn.esprit.skillmatch.messagerie.dto.ConversationResponse;
import tn.esprit.skillmatch.messagerie.dto.ConversationModerationRequest;
import tn.esprit.skillmatch.messagerie.dto.CreateConversationRequest;
import tn.esprit.skillmatch.messagerie.dto.FichierUploadResponse;
import tn.esprit.skillmatch.messagerie.dto.MessageResponse;
import tn.esprit.skillmatch.messagerie.dto.MessagerieAdminDashboardResponse;
import tn.esprit.skillmatch.messagerie.dto.MessagerieAdminStatsResponse;
import tn.esprit.skillmatch.messagerie.dto.ParticipantSummary;
import tn.esprit.skillmatch.messagerie.dto.SignalementResponse;
import tn.esprit.skillmatch.messagerie.entity.AuditLog;
import tn.esprit.skillmatch.messagerie.entity.Conversation;
import tn.esprit.skillmatch.messagerie.entity.Message;
import tn.esprit.skillmatch.messagerie.entity.Signalement;
import tn.esprit.skillmatch.messagerie.exception.AccessForbiddenException;
import tn.esprit.skillmatch.messagerie.exception.BadRequestException;
import tn.esprit.skillmatch.messagerie.exception.ConflictException;
import tn.esprit.skillmatch.messagerie.exception.ResourceNotFoundException;
import tn.esprit.skillmatch.messagerie.repository.AuditLogRepository;
import tn.esprit.skillmatch.messagerie.repository.ConversationRepository;
import tn.esprit.skillmatch.messagerie.repository.MessageRepository;
import tn.esprit.skillmatch.messagerie.repository.SignalementRepository;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.Normalizer;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ConversationService {

    public static final String MSG_FORBIDDEN_ADMIN = "Vous ne pouvez pas écrire à un administrateur.";
    private static final long MAX_FILE_SIZE_BYTES = 10L * 1024 * 1024;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "jpg", "jpeg", "png", "doc", "docx");

    private final ConversationRepository conversationRepository;
    private final ObjectMapper objectMapper;
    private final MessageRepository messageRepository;
    private final UserClient userClient;
    private final SignalementRepository signalementRepository;
    private final AuditLogRepository auditLogRepository;
    @Value("${messagerie.fichiers.dir:./uploads}")
    private String fichiersDir = "./uploads";

    public ConversationService(ConversationRepository conversationRepository,
                               ObjectMapper objectMapper,
                               MessageRepository messageRepository,
                               UserClient userClient) {
        this(conversationRepository, objectMapper, messageRepository, userClient, null, null);
    }

    @Autowired
    public ConversationService(ConversationRepository conversationRepository,
                               ObjectMapper objectMapper,
                               MessageRepository messageRepository,
                               UserClient userClient,
                               SignalementRepository signalementRepository,
                               AuditLogRepository auditLogRepository) {
        this.conversationRepository = conversationRepository;
        this.objectMapper = objectMapper;
        this.messageRepository = messageRepository;
        this.userClient = userClient;
        this.signalementRepository = signalementRepository;
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * Crée une conversation directe ou un groupe.
     * Règles :
     * - Au moins 2 participants distincts (3 pour un groupe).
     * - Le créateur (user authentifié) doit figurer dans la liste.
     * - Si type=GROUPE : nomGroupe obligatoire (3-50 caractères).
     * - Un Candidat ou Recruteur ne peut pas inclure un Admin (403).
     * - Les ids sont triés pour la détection de doublons (409).
     */
    @Transactional
    public ConversationResponse create(CreateConversationRequest request, AuthenticatedUser user) {
        List<Long> rawOrderIds = request.participantIds().stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        if (rawOrderIds.size() < 2) {
            throw new BadRequestException("Il faut au moins 2 participants distincts");
        }
        if (rawOrderIds.stream().anyMatch(participantId -> participantId <= 0)) {
            throw new BadRequestException("Chaque identifiant de participant doit être positif");
        }
        if (!rawOrderIds.contains(user.id())) {
            throw new BadRequestException("Vous devez faire partie des participants de la conversation.");
        }

        Conversation.Type type = "GROUPE".equalsIgnoreCase(request.type())
                ? Conversation.Type.GROUPE
                : Conversation.Type.DIRECT;

        String nomGroupe = null;
        if (type == Conversation.Type.GROUPE) {
            if (rawOrderIds.size() < 3) {
                throw new BadRequestException("Un groupe doit contenir au moins 3 participants (vous et 2 personnes).");
            }
            nomGroupe = validateGroupName(request.nomGroupe(), "Un nom de groupe est obligatoire.");
        }

        enforceNoAdminForNonAdmin(rawOrderIds, user);

        List<Long> sortedIds = new ArrayList<>(rawOrderIds);
        Collections.sort(sortedIds);

        String sortedJson = toJson(sortedIds);
        conversationRepository.findByParticipantIds(sortedJson).ifPresent(existing -> {
            throw new ConflictException("Une conversation existe déjà entre ces participants.");
        });

        List<Long> ordre = new ArrayList<>();
        ordre.add(user.id());
        rawOrderIds.stream().filter(id -> !id.equals(user.id())).forEach(ordre::add);

        Conversation conversation = new Conversation(
                UUID.randomUUID().toString(),
                sortedIds,
                user.id(),
                nomGroupe,
                type
        );
        conversation.setOrdreParticipants(ordre);
        if (type == Conversation.Type.GROUPE) {
            conversation.setDernierMessageAt(LocalDateTime.now());
        }

        Conversation saved = conversationRepository.save(conversation);
        Map<Long, UserClient.UserProfile> usersMap = resolveUsersMap(sortedIds, user);

        if (type == Conversation.Type.GROUPE) {
            String creatorFirst = firstNameFor(user.id(), usersMap);
            saveSystemMessage(saved, user.id(), creatorFirst + " a créé le groupe");
        }

        return toResponse(saved, user, usersMap);
    }

    /**
     * Liste les conversations auxquelles l'utilisateur courant participe (y compris pour un ADMIN).
     */
    public List<ConversationResponse> findByUser(AuthenticatedUser user) {
        List<Conversation> conversations = conversationRepository.findByParticipantId(user.id().toString());
        if (conversations.isEmpty()) {
            return List.of();
        }
        Set<Long> allParticipantIds = new LinkedHashSet<>();
        for (Conversation c : conversations) {
            if (c.getParticipantIds() != null) {
                allParticipantIds.addAll(c.getParticipantIds());
            }
        }
        Map<Long, UserClient.UserProfile> usersMap = resolveUsersMap(allParticipantIds, user);
        return conversations.stream()
                .map(conversation -> toResponse(conversation, user, usersMap))
                .collect(Collectors.toList());
    }

    /**
     * Récupère une conversation par son id.
     * Vérifie que l'utilisateur est participant (sauf ADMIN).
     */
    public ConversationResponse findById(String id, AuthenticatedUser user) {
        Conversation conversation = getConversationOrThrow(id);
        checkParticipant(conversation, user);
        Map<Long, UserClient.UserProfile> usersMap = resolveUsersMap(conversation.getParticipantIds(), user);
        return toResponse(conversation, user, usersMap);
    }

    /**
     * Recherche de personnes par nom (10 résultats max) via l'annuaire existant (UserClient),
     * sans exposer d'e-mail. Masque les administrateurs aux rôles CANDIDAT et RECRUTEUR.
     * Retourne les contacts récents lorsque la recherche est vide.
     */
    @Transactional(readOnly = true)
    public List<ParticipantSummary> searchUsers(String query, String role, AuthenticatedUser user) {
        if (user == null || user.id() == null) {
            throw new AccessForbiddenException("Authentification requise.");
        }
        String normalizedRole = (role != null && !role.isBlank() && !"ALL".equalsIgnoreCase(role))
                ? role.trim().toUpperCase(Locale.ROOT)
                : null;
        if (!user.isAdmin() && "ADMIN".equals(normalizedRole)) {
            return List.of();
        }

        String trimmedQuery = query != null ? query.trim() : "";
        if (trimmedQuery.length() < 2) {
            List<Conversation> convs = conversationRepository.findByParticipantId(user.id().toString());
            Set<Long> recentIds = new LinkedHashSet<>();
            for (Conversation c : convs) {
                if (c.getParticipantIds() != null) {
                    for (Long pid : c.getParticipantIds()) {
                        if (pid != null && !pid.equals(user.id())) {
                            recentIds.add(pid);
                        }
                    }
                }
            }
            Map<Long, UserClient.UserProfile> usersMap = resolveUsersMap(recentIds, user);
            String normFilter = normalizeAccentFree(trimmedQuery);
            return recentIds.stream()
                    .map(usersMap::get)
                    .filter(Objects::nonNull)
                    .filter(p -> user.isAdmin() || !"ADMIN".equalsIgnoreCase(p.role()))
                    .filter(p -> normalizedRole == null || normalizedRole.equalsIgnoreCase(p.role()))
                    .filter(p -> normFilter.isEmpty() || normalizeAccentFree(p.nom()).contains(normFilter))
                    .limit(10)
                    .map(p -> new ParticipantSummary(p.id(), p.nom(), p.role()))
                    .toList();
        }

        if (userClient == null) {
            return List.of();
        }
        List<UserClient.UserProfile> found = userClient.searchUsers(trimmedQuery, normalizedRole, user.token());
        return found.stream()
                .filter(p -> p.id() != null && !p.id().equals(user.id()))
                .filter(p -> user.isAdmin() || !"ADMIN".equalsIgnoreCase(p.role()))
                .filter(p -> normalizedRole == null || normalizedRole.equalsIgnoreCase(p.role()))
                .sorted(Comparator.comparing(p -> normalizeAccentFree(p.nom())))
                .limit(10)
                .map(p -> new ParticipantSummary(p.id(), p.nom(), p.role()))
                .toList();
    }

    @Transactional(readOnly = true)
    public MessagerieAdminDashboardResponse getAdminDashboard(String scope, String query, AuthenticatedUser user) {
        return getAdminDashboard(scope, query, null, null, null, null, user);
    }

    @Transactional(readOnly = true)
    public MessagerieAdminDashboardResponse getAdminDashboard(String scope,
                                                              String query,
                                                              String type,
                                                              String statut,
                                                              AuthenticatedUser user) {
        return getAdminDashboard(scope, query, type, statut, null, null, user);
    }

    @Transactional(readOnly = true)
    public MessagerieAdminDashboardResponse getAdminDashboard(String scope,
                                                              String query,
                                                              String type,
                                                              String statut,
                                                              Integer page,
                                                              Integer size,
                                                              AuthenticatedUser user) {
        ensureAdmin(user);

        List<Conversation> conversations = conversationRepository.findAllByOrderByDernierMessageAtDescDateCreationDesc();
        MessagerieAdminStatsResponse stats = buildAdminStats(conversations);
        if (conversations.isEmpty()) {
            return new MessagerieAdminDashboardResponse(stats, List.of(), 0L, 0, 0, size != null && size > 0 ? size : 10);
        }

        Set<Long> participantIds = new LinkedHashSet<>();
        for (Conversation conversation : conversations) {
            if (conversation.getParticipantIds() != null) {
                participantIds.addAll(conversation.getParticipantIds());
            }
        }
        Map<Long, UserClient.UserProfile> usersMap = resolveUsersMap(participantIds, user);

        List<AdminConversationSummaryResponse> rows = conversations.stream()
                .map(conversation -> toAdminSummary(conversation, user, usersMap))
                .filter(row -> matchesScope(row, scope))
                .filter(row -> matchesType(row, type))
                .filter(row -> matchesStatut(row, statut))
                .filter(row -> matchesQuery(row, query))
                .toList();

        if (page == null && size == null) {
            return new MessagerieAdminDashboardResponse(stats, rows);
        }

        int safeSize = size != null && size > 0 ? Math.min(size, 100) : 10;
        int safePage = page != null && page >= 0 ? page : 0;
        long totalElements = rows.size();
        int totalPages = totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / safeSize);
        int fromIndex = Math.min(safePage * safeSize, rows.size());
        int toIndex = Math.min(fromIndex + safeSize, rows.size());
        List<AdminConversationSummaryResponse> pagedRows = rows.subList(fromIndex, toIndex);

        return new MessagerieAdminDashboardResponse(stats, pagedRows, totalElements, totalPages, safePage, safeSize);
    }

    @Transactional
    public AdminConversationDetailResponse getAdminConversationDetail(String id, AuthenticatedUser user) {
        ensureAdmin(user);

        Conversation conversation = getConversationOrThrow(id);
        List<Message> messages = messageRepository != null
                ? messageRepository.findByConversationIdOrderByDateAsc(conversation.getId())
                : List.of();
        List<Signalement> signalements = signalementRepository != null
                ? signalementRepository.findByConversationIdOrderByDateSignalementDesc(conversation.getId())
                : List.of();
        List<AuditLog> existingAudits = auditLogRepository != null
                ? auditLogRepository.findByConversationIdOrderByDateActionDesc(conversation.getId())
                : List.of();

        Set<Long> userIds = new LinkedHashSet<>();
        if (conversation.getParticipantIds() != null) {
            userIds.addAll(conversation.getParticipantIds());
        }
        if (conversation.getCandidatUserId() != null) {
            userIds.add(conversation.getCandidatUserId());
        }
        if (user.id() != null) {
            userIds.add(user.id());
        }
        for (Message message : messages) {
            if (!message.estSysteme() && message.getSenderId() != null && message.getSenderId() > 0) {
                userIds.add(message.getSenderId());
            }
        }
        for (Signalement s : signalements) {
            if (s.getAuteurSignalementId() != null) userIds.add(s.getAuteurSignalementId());
            if (s.getAuteurMessageId() != null) userIds.add(s.getAuteurMessageId());
            if (s.getAdminId() != null) userIds.add(s.getAdminId());
        }
        Map<Long, UserClient.UserProfile> usersMap = resolveUsersMap(userIds, user);

        List<AuditLog> newAudits = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        UserClient.UserProfile adminProfile = usersMap.get(user.id());
        String adminName = adminProfile != null ? adminProfile.nom() : "Administrateur";

        newAudits.add(recordAudit(
                conversation.getId(),
                null,
                null,
                user.id(),
                adminName,
                "ADMIN",
                "CONSULTATION_DETAILS",
                "Consultation de la fiche conversation",
                now
        ));

        Map<String, Message> messageById = messages.stream()
                .collect(Collectors.toMap(Message::getId, m -> m, (a, b) -> a));

        for (Message m : messages) {
            if (Boolean.TRUE.equals(m.getSignalee())) {
                newAudits.add(recordAudit(
                        conversation.getId(),
                        m.getId(),
                        null,
                        user.id(),
                        adminName,
                        "ADMIN",
                        "CONSULTATION_MESSAGE_SIGNALE",
                        "Consultation du contenu d'un message signalé",
                        now
                ));
            }
        }

        AdminConversationSummaryResponse summary = toAdminSummary(conversation, user, usersMap);
        List<MessageResponse> messageResponses = messages.stream()
                .map(message -> toAdminMessageResponse(message, usersMap))
                .toList();

        List<SignalementResponse> signalementResponses = signalements.stream()
                .map(s -> toSignalementResponse(s, messageById.get(s.getMessageId()), usersMap, true))
                .toList();

        List<AuditLog> combinedAudits = new ArrayList<>();
        for (AuditLog a : newAudits) {
            if (a != null) combinedAudits.add(a);
        }
        combinedAudits.addAll(existingAudits);
        List<AuditEntryResponse> auditResponses = combinedAudits.stream()
                .map(this::toAuditEntryResponse)
                .toList();

        UserClient.UserProfile creatorProfile = usersMap.get(conversation.getCandidatUserId());
        String createurNom = creatorProfile != null ? creatorProfile.nom() : "Membre";

        return new AdminConversationDetailResponse(
                summary,
                conversation.getDateCreation(),
                conversation.getDateArchivage(),
                conversation.getDateSignalement(),
                conversation.getCandidatUserId(),
                messageResponses,
                createurNom,
                summary.messages(),
                summary.nonLusCount(),
                signalementResponses,
                auditResponses
        );
    }

    @Transactional
    public AdminConversationSummaryResponse updateModeration(String id,
                                                            ConversationModerationRequest request,
                                                            AuthenticatedUser user) {
        ensureAdmin(user);
        if (request == null || (request.archivee() == null && request.signalee() == null)) {
            throw new BadRequestException("Au moins un statut de modération doit être fourni.");
        }

        Conversation conversation = getConversationOrThrow(id);
        LocalDateTime now = LocalDateTime.now();

        if (request.archivee() != null) {
            conversation.setArchivee(request.archivee());
            conversation.setDateArchivage(Boolean.TRUE.equals(request.archivee()) ? now : null);
        }
        if (request.signalee() != null) {
            conversation.setSignalee(request.signalee());
            conversation.setDateSignalement(Boolean.TRUE.equals(request.signalee()) ? now : null);
        }

        Conversation saved = conversationRepository.save(conversation);
        Set<Long> idsToResolve = new LinkedHashSet<>(saved.getParticipantIds());
        if (user.id() != null) idsToResolve.add(user.id());
        Map<Long, UserClient.UserProfile> usersMap = resolveUsersMap(idsToResolve, user);
        UserClient.UserProfile adminProfile = usersMap.get(user.id());
        String actionName = request.archivee() != null
                ? (Boolean.TRUE.equals(request.archivee()) ? "ARCHIVAGE" : "DESARCHIVAGE")
                : "MODERATION";
        recordAudit(
                saved.getId(),
                null,
                null,
                user.id(),
                adminProfile != null ? adminProfile.nom() : "Administrateur",
                "ADMIN",
                actionName,
                Boolean.TRUE.equals(request.archivee()) ? "Conversation archivée" : "Statut de modération mis à jour",
                now
        );

        return toAdminSummary(saved, user, usersMap);
    }

    @Transactional
    public String exportSingleConversationCsv(String id, AuthenticatedUser user) {
        ensureAdmin(user);
        Conversation conversation = getConversationOrThrow(id);
        Set<Long> userIds = new LinkedHashSet<>(conversation.getParticipantIds());
        if (user.id() != null) userIds.add(user.id());
        Map<Long, UserClient.UserProfile> usersMap = resolveUsersMap(userIds, user);
        AdminConversationSummaryResponse row = toAdminSummary(conversation, user, usersMap);

        UserClient.UserProfile adminProfile = usersMap.get(user.id());
        recordAudit(
                conversation.getId(),
                null,
                null,
                user.id(),
                adminProfile != null ? adminProfile.nom() : "Administrateur",
                "ADMIN",
                "EXPORT_CSV",
                "Export CSV de la conversation",
                LocalDateTime.now()
        );

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String participantsLabels = row.participants() == null ? "" : row.participants().stream()
                .map(p -> p.nom() + (p.role() != null ? " (" + p.role() + ")" : ""))
                .collect(Collectors.joining(", "));
        String formattedDate = row.derniereActivite() != null ? row.derniereActivite().format(formatter) : "";

        return "\uFEFFConversation;Type;Participants;Messages;Non lus;Derniere activite;Statut;Signalements\r\n"
                + csvValue(row.conversation()) + ';'
                + csvValue(row.type()) + ';'
                + csvValue(participantsLabels) + ';'
                + row.messages() + ';'
                + row.nonLusCount() + ';'
                + csvValue(formattedDate) + ';'
                + csvValue(row.statut()) + ';'
                + row.signalementsCount()
                + "\r\n";
    }

    @Transactional(readOnly = true)
    public String exportAdminDashboardCsv(String scope, String query, AuthenticatedUser user) {
        return exportAdminDashboardCsv(scope, query, null, null, user);
    }

    @Transactional(readOnly = true)
    public String exportAdminDashboardCsv(String scope,
                                          String query,
                                          String type,
                                          String statut,
                                          AuthenticatedUser user) {
        MessagerieAdminDashboardResponse dashboard = getAdminDashboard(scope, query, type, statut, user);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        StringBuilder csv = new StringBuilder("\uFEFFID;Conversation;Type;Membres;Participants;Messages;Derniere activite;Statut;Archivee;Signalee;Non lus;Dernier message\r\n");
        for (AdminConversationSummaryResponse row : dashboard.conversations()) {
            String participantsNames = row.participants() == null ? "" : row.participants().stream()
                    .map(ParticipantSummary::nom)
                    .filter(Objects::nonNull)
                    .filter(nom -> !nom.isBlank())
                    .collect(Collectors.joining(", "));
            String formattedDate = row.derniereActivite() != null ? row.derniereActivite().format(formatter) : "";
            csv.append(csvValue(row.id())).append(';')
                    .append(csvValue(row.conversation())).append(';')
                    .append(csvValue(row.type())).append(';')
                    .append(row.membres()).append(';')
                    .append(csvValue(participantsNames)).append(';')
                    .append(row.messages()).append(';')
                    .append(csvValue(formattedDate)).append(';')
                    .append(csvValue(row.statut())).append(';')
                    .append(row.archivee() ? "Oui" : "Non").append(';')
                    .append(row.signalee() ? "Oui" : "Non").append(';')
                    .append(row.nonLusCount()).append(';')
                    .append(csvValue(row.dernierMessageContenu()))
                    .append("\r\n");
        }
        return csv.toString();
    }

    /**
     * Expose l'entité Conversation pour MessageService.
     */
    public Conversation getEntityById(String id) {
        return getConversationOrThrow(id);
    }

    /**
     * Remplace la liste des participants. Seul le créateur (ou ADMIN) peut modifier.
     * Le créateur doit rester participant et une conversation doit garder au moins 2 membres.
     * Passage direct → groupe : nomGroupe requis.
     * Passage groupe → direct : uniquement si explicitement demandé (type=direct) avec 2 participants.
     */
    @Transactional
    public ConversationResponse updateParticipants(String id, CreateConversationRequest request, AuthenticatedUser user) {
        Conversation conversation = getConversationOrThrow(id);
        checkManager(conversation, user);

        List<Long> previousIds = conversation.getParticipantIds() != null
                ? new ArrayList<>(conversation.getParticipantIds())
                : new ArrayList<>();

        List<Long> ids = request.participantIds().stream()
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        if (ids.size() < 2) {
            throw new BadRequestException("Il faut au moins 2 participants distincts");
        }
        if (ids.stream().anyMatch(participantId -> participantId <= 0)) {
            throw new BadRequestException("Chaque identifiant de participant doit être positif");
        }
        if (!ids.contains(conversation.getCandidatUserId())) {
            throw new BadRequestException("Le créateur doit rester participant de la conversation.");
        }

        boolean wasGroupe = conversation.estGroupe();
        boolean explicitDirect = "DIRECT".equalsIgnoreCase(request.type());
        boolean willBeGroupe;
        String nomGroupe = conversation.getNomGroupe();

        if (explicitDirect) {
            if (ids.size() != 2) {
                throw new BadRequestException("Une conversation directe doit contenir exactement 2 participants.");
            }
            willBeGroupe = false;
            nomGroupe = null;
        } else if (!wasGroupe && (ids.size() >= 3 || "GROUPE".equalsIgnoreCase(request.type()))) {
            if (ids.size() < 3) {
                throw new BadRequestException("Un groupe doit contenir au moins 3 participants.");
            }
            willBeGroupe = true;
            nomGroupe = validateGroupName(request.nomGroupe(), "Un nom de groupe est obligatoire pour créer un groupe.");
        } else {
            willBeGroupe = wasGroupe;
            if (willBeGroupe && request.nomGroupe() != null && !request.nomGroupe().trim().isEmpty()) {
                nomGroupe = validateGroupName(request.nomGroupe(), "Un nom de groupe est obligatoire.");
            } else if (!willBeGroupe) {
                nomGroupe = null;
            }
        }

        enforceNoAdminForNonAdmin(ids, user);

        String sortedJson = toJson(ids);
        conversationRepository.findByParticipantIds(sortedJson).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new ConflictException("Une conversation existe déjà entre ces participants.");
            }
        });

        // Mettre à jour l'ordre des participants (les existants restent dans l'ordre, les nouveaux sont ajoutés à la fin)
        List<Long> currentOrder = conversation.getOrdreParticipants() != null && !conversation.getOrdreParticipants().isEmpty()
                ? new ArrayList<>(conversation.getOrdreParticipants())
                : new ArrayList<>(previousIds);
        List<Long> newOrder = currentOrder.stream()
                .filter(ids::contains)
                .collect(Collectors.toCollection(ArrayList::new));
        for (Long rawId : request.participantIds()) {
            if (rawId != null && ids.contains(rawId) && !newOrder.contains(rawId)) {
                newOrder.add(rawId);
            }
        }
        for (Long sortedId : ids) {
            if (!newOrder.contains(sortedId)) {
                newOrder.add(sortedId);
            }
        }

        Set<Long> beforeSet = new LinkedHashSet<>(previousIds);
        Set<Long> afterSet = new LinkedHashSet<>(ids);
        List<Long> addedIds = ids.stream().filter(pid -> !beforeSet.contains(pid)).toList();
        List<Long> removedIds = previousIds.stream().filter(pid -> !afterSet.contains(pid)).toList();

        conversation.setParticipantIds(ids);
        conversation.setOrdreParticipants(newOrder);
        conversation.setNomGroupe(nomGroupe);
        conversation.setType(willBeGroupe ? Conversation.Type.GROUPE.name() : Conversation.Type.DIRECT.name());

        Conversation saved = conversationRepository.save(conversation);

        Set<Long> allToResolve = new LinkedHashSet<>(previousIds);
        allToResolve.addAll(ids);
        allToResolve.add(user.id());
        Map<Long, UserClient.UserProfile> usersMap = resolveUsersMap(allToResolve, user);
        String actorFirst = firstNameFor(user.id(), usersMap);

        if (willBeGroupe || (wasGroupe && !willBeGroupe)) {
            for (Long addedId : addedIds) {
                saveSystemMessage(saved, user.id(), actorFirst + " a ajouté " + firstNameFor(addedId, usersMap));
            }
            for (Long removedId : removedIds) {
                saveSystemMessage(saved, user.id(), actorFirst + " a retiré " + firstNameFor(removedId, usersMap));
            }
            if (wasGroupe && !willBeGroupe) {
                saveSystemMessage(saved, user.id(), actorFirst + " a converti le groupe en conversation directe");
            }
        }

        return toResponse(saved, user, usersMap);
    }

    /**
     * Permet à tout membre d'un groupe (y compris le créateur) de quitter le groupe.
     * - Refuse (400) de quitter une conversation directe.
     * - Si le créateur quitte et qu'il reste au moins 2 membres, le rôle de créateur passe
     *   au plus ancien participant (jamais un Admin si d'autres rôles existent).
     * - S'il reste moins de 2 membres, refuse (400) en invitant à convertir en directe ou supprimer.
     */
    @Transactional
    public ConversationResponse quitterGroupe(String id, AuthenticatedUser user) {
        Conversation conversation = getConversationOrThrow(id);
        if (!conversation.estGroupe()) {
            throw new BadRequestException("Impossible de quitter une conversation directe.");
        }
        if (!conversation.estParticipant(user.id())) {
            throw new AccessForbiddenException("Vous n'êtes pas participant de cette conversation.");
        }

        List<Long> currentOrder = conversation.getOrdreParticipants() != null && !conversation.getOrdreParticipants().isEmpty()
                ? new ArrayList<>(conversation.getOrdreParticipants())
                : new ArrayList<>(conversation.getParticipantIds());

        List<Long> remainingOrder = currentOrder.stream()
                .filter(pid -> !pid.equals(user.id()))
                .collect(Collectors.toList());
        for (Long pid : conversation.getParticipantIds()) {
            if (!pid.equals(user.id()) && !remainingOrder.contains(pid)) {
                remainingOrder.add(pid);
            }
        }

        if (remainingOrder.size() < 2) {
            throw new BadRequestException(
                    "Il resterait moins de 2 membres : convertissez le groupe en conversation directe ou supprimez la conversation.");
        }

        List<Long> remainingSorted = remainingOrder.stream().sorted().collect(Collectors.toList());

        Set<Long> allToResolve = new LinkedHashSet<>(conversation.getParticipantIds());
        allToResolve.add(user.id());
        Map<Long, UserClient.UserProfile> usersMap = resolveUsersMap(allToResolve, user);

        if (user.id().equals(conversation.getCandidatUserId())) {
            Long nextCreator = selectNextCreator(remainingOrder, usersMap, user.token());
            conversation.setCandidatUserId(nextCreator);
        }

        conversation.setParticipantIds(remainingSorted);
        conversation.setOrdreParticipants(remainingOrder);
        Conversation saved = conversationRepository.save(conversation);

        String leaverFirst = firstNameFor(user.id(), usersMap);
        saveSystemMessage(saved, user.id(), leaverFirst + " a quitté le groupe");

        return toResponse(saved, user, usersMap);
    }

    /**
     * Supprime une conversation et ses messages. Seul le créateur ou ADMIN peut supprimer.
     */
    @Transactional
    public void delete(String id, AuthenticatedUser user) {
        Conversation conversation = getConversationOrThrow(id);
        checkManager(conversation, user);
        if (signalementRepository != null) {
            signalementRepository.deleteByConversationId(id);
        }
        if (auditLogRepository != null) {
            auditLogRepository.deleteByConversationId(id);
        }
        messageRepository.deleteByConversationId(id);
        conversationRepository.delete(conversation);
    }

    /**
     * Stocke un fichier joint sur le disque de messagerie-service et retourne son URL protégée.
     * Valide taille <= 10 Mo, nom assaini sans traversal et extension autorisée.
     */
    public FichierUploadResponse uploadFichier(String conversationId, MultipartFile fichier, AuthenticatedUser user) {
        Conversation conversation = getConversationOrThrow(conversationId);
        checkParticipant(conversation, user);

        if (fichier == null || fichier.isEmpty()) {
            throw new BadRequestException("Aucun fichier fourni.");
        }
        if (fichier.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new BadRequestException("Le fichier est trop lourd (10 Mo maximum).");
        }

        String originalName = sanitizeOriginalFilename(fichier.getOriginalFilename());
        String extension = extractExtension(originalName);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BadRequestException("Type de fichier non autorisé. Extensions acceptées : pdf, jpg, jpeg, png, doc, docx.");
        }

        try {
            Path dir = Paths.get(fichiersDir).toAbsolutePath().normalize();
            Files.createDirectories(dir);

            String safeBase = originalName.replaceAll("[^a-zA-Z0-9._-]", "_");
            String nomStockage = UUID.randomUUID() + "_" + safeBase;
            Path cible = dir.resolve(nomStockage).normalize();
            if (!cible.startsWith(dir)) {
                throw new BadRequestException("Nom de fichier invalide.");
            }

            Files.copy(fichier.getInputStream(), cible, StandardCopyOption.REPLACE_EXISTING);

            String url = "/skillmatch/messagerie/conversations/" + conversationId + "/fichiers/" + nomStockage;
            return new FichierUploadResponse(url, originalName, fichier.getSize());
        } catch (BadRequestException e) {
            throw e;
        } catch (IOException e) {
            throw new BadRequestException("Impossible d'enregistrer le fichier : " + e.getMessage());
        }
    }

    /**
     * Charge un fichier joint protégé après vérification des droits et du chemin.
     */
    public Resource loadFichier(String conversationId, String filename, AuthenticatedUser user) {
        Conversation conversation = getConversationOrThrow(conversationId);
        checkParticipant(conversation, user);

        if (filename == null || filename.isBlank() || filename.contains("..")
                || filename.contains("/") || filename.contains("\\")) {
            throw new BadRequestException("Nom de fichier invalide.");
        }

        try {
            Path dir = Paths.get(fichiersDir).toAbsolutePath().normalize();
            Path cible = dir.resolve(filename).normalize();
            if (!cible.startsWith(dir) || !Files.exists(cible) || !Files.isRegularFile(cible)) {
                throw new ResourceNotFoundException("Fichier introuvable.");
            }
            return new UrlResource(cible.toUri());
        } catch (MalformedURLException e) {
            throw new ResourceNotFoundException("Fichier introuvable.");
        }
    }

    private Long selectNextCreator(List<Long> remainingOrder, Map<Long, UserClient.UserProfile> usersMap, String token) {
        for (Long candidateId : remainingOrder) {
            String role = null;
            UserClient.UserProfile profile = usersMap.get(candidateId);
            if (profile != null) {
                role = profile.role();
            } else if (userClient != null && token != null && !token.isBlank()) {
                try {
                    role = userClient.resolveRole(candidateId, token);
                } catch (Exception ignored) {}
            }
            if (!"ADMIN".equalsIgnoreCase(role)) {
                return candidateId;
            }
        }
        return remainingOrder.get(0);
    }

    private void enforceNoAdminForNonAdmin(List<Long> ids, AuthenticatedUser user) {
        if (user.isAdmin() || userClient == null) {
            return;
        }
        for (Long participantId : ids) {
            if (participantId.equals(user.id())) continue;
            String role = userClient.resolveRole(participantId, user.token());
            if ("ADMIN".equalsIgnoreCase(role)) {
                throw new AccessForbiddenException(MSG_FORBIDDEN_ADMIN);
            }
        }
    }

    private String validateGroupName(String rawName, String requiredMessage) {
        if (rawName == null || rawName.trim().isEmpty()) {
            throw new BadRequestException(requiredMessage);
        }
        String trimmed = rawName.trim();
        if (trimmed.length() < 3 || trimmed.length() > 50) {
            throw new BadRequestException("Le nom du groupe doit contenir entre 3 et 50 caractères.");
        }
        return trimmed;
    }

    private String sanitizeOriginalFilename(String rawFilename) {
        if (rawFilename == null || rawFilename.isBlank()) {
            throw new BadRequestException("Nom de fichier invalide.");
        }
        String normalizedSlashes = rawFilename.replace('\\', '/');
        if (normalizedSlashes.contains("..")) {
            throw new BadRequestException("Nom de fichier invalide.");
        }
        String baseName = Paths.get(normalizedSlashes).getFileName().toString().trim();
        if (baseName.isEmpty() || baseName.startsWith(".") || baseName.contains("..")) {
            throw new BadRequestException("Nom de fichier invalide.");
        }
        return baseName;
    }

    private String extractExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        if (lastDot < 0 || lastDot == filename.length() - 1) {
            return "";
        }
        return filename.substring(lastDot + 1).toLowerCase(Locale.ROOT);
    }

    private void saveSystemMessage(Conversation conversation, Long actorUserId, String content) {
        if (messageRepository == null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        Message systemMessage = new Message(
                UUID.randomUUID().toString(),
                content,
                Message.TYPE_SYSTEME,
                now,
                true,
                conversation,
                actorUserId != null ? actorUserId : 0L,
                null,
                null,
                null
        );
        messageRepository.save(systemMessage);
        conversation.setDernierMessageAt(now);
        conversationRepository.save(conversation);
    }

    private Map<Long, UserClient.UserProfile> resolveUsersMap(java.util.Collection<Long> ids, AuthenticatedUser user) {
        if (userClient == null || ids == null || ids.isEmpty()) {
            return Map.of();
        }
        return userClient.resolveUsers(ids, user != null ? user.token() : null);
    }

    private String firstNameFor(Long userId, Map<Long, UserClient.UserProfile> usersMap) {
        UserClient.UserProfile profile = usersMap.get(userId);
        if (profile != null) {
            return profile.firstName();
        }
        return "Un membre";
    }

    private MessagerieAdminStatsResponse buildAdminStats(List<Conversation> conversations) {
        long directCount = conversations.stream().filter(conversation -> !conversation.estGroupe()).count();
        long groupCount = conversations.stream().filter(Conversation::estGroupe).count();
        long pendingFlagsFromConvs = conversations.stream()
                .filter(conversation -> Boolean.TRUE.equals(conversation.getSignalee()))
                .filter(conversation -> !Boolean.TRUE.equals(conversation.getArchivee()))
                .count();
        long pendingFlags = (signalementRepository != null && signalementRepository.count() > 0)
                ? signalementRepository.countByStatut(Signalement.STATUT_EN_ATTENTE)
                : pendingFlagsFromConvs;

        LocalDate today = LocalDate.now();
        LocalDateTime startOfWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atStartOfDay();
        long newConversationsWeek = conversations.stream()
                .filter(conversation -> !conversation.estGroupe())
                .filter(conversation -> conversation.getDateCreation() != null && !conversation.getDateCreation().isBefore(startOfWeek))
                .count();
        long newGroupsWeek = conversations.stream()
                .filter(Conversation::estGroupe)
                .filter(conversation -> conversation.getDateCreation() != null && !conversation.getDateCreation().isBefore(startOfWeek))
                .count();

        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay();
        long messagesToday = messageRepository != null
                ? messageRepository.countByDateGreaterThanEqualAndDateLessThan(start, end)
                : 0L;
        List<Message> todaysMessages = messageRepository != null
                ? messageRepository.findByDateGreaterThanEqualAndDateLessThan(start, end)
                : List.of();
        if (messagesToday == 0L && todaysMessages != null && !todaysMessages.isEmpty()) {
            messagesToday = todaysMessages.size();
        }
        Integer peakHour = computePeakHour(todaysMessages);
        String peakHourLabel = peakHour != null ? "Pic à " + peakHour + " h" : "Aucune activité";

        return new MessagerieAdminStatsResponse(
                directCount,
                groupCount,
                messagesToday,
                pendingFlags,
                newConversationsWeek,
                newGroupsWeek,
                peakHour,
                peakHourLabel
        );
    }

    private Integer computePeakHour(List<Message> messages) {
        if (messages == null || messages.isEmpty()) {
            return null;
        }
        Map<Integer, Long> countsByHour = messages.stream()
                .map(Message::getDate)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(LocalDateTime::getHour, Collectors.counting()));
        return countsByHour.entrySet().stream()
                .max(Comparator.<Map.Entry<Integer, Long>>comparingLong(Map.Entry::getValue)
                        .thenComparing(Map.Entry.<Integer, Long>comparingByKey().reversed()))
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    private MessageResponse toAdminMessageResponse(Message message, Map<Long, UserClient.UserProfile> usersMap) {
        String senderNom = null;
        if (!message.estSysteme() && message.getSenderId() != null && message.getSenderId() > 0) {
            UserClient.UserProfile profile = usersMap.get(message.getSenderId());
            senderNom = profile != null ? profile.nom() : "Membre";
        }
        boolean signalee = Boolean.TRUE.equals(message.getSignalee());
        boolean masque = Boolean.TRUE.equals(message.getMasque());
        boolean visibleContent = signalee || message.estSysteme();
        String content = visibleContent ? message.getContenu() : "[Contenu masqué]";
        return new MessageResponse(
                message.getId(),
                content,
                message.getType(),
                message.getDate(),
                message.getLu(),
                message.getConversation() != null ? message.getConversation().getId() : null,
                message.getSenderId(),
                senderNom,
                visibleContent ? message.getFichierNom() : null,
                visibleContent ? message.getFichierUrl() : null,
                visibleContent ? message.getFichierTaille() : null,
                signalee,
                masque
        );
    }

    private AdminConversationSummaryResponse toAdminSummary(Conversation conversation,
                                                            AuthenticatedUser user,
                                                            Map<Long, UserClient.UserProfile> usersMap) {
        Optional<Message> latest = latestMessage(conversation.getId());
        List<ParticipantSummary> participants = resolveParticipantsOrdered(conversation, usersMap);
        String title = resolveConversationLabel(conversation, participants);
        long messagesCount = messageRepository != null ? messageRepository.countByConversationId(conversation.getId()) : 0L;
        LocalDateTime activityAt = conversation.getDernierMessageAt() != null
                ? conversation.getDernierMessageAt()
                : conversation.getDateCreation();
        long sigCount = signalementRepository != null
                ? signalementRepository.countByConversationId(conversation.getId())
                : 0L;
        if (sigCount == 0L && Boolean.TRUE.equals(conversation.getSignalee())) {
            sigCount = 1L;
        }
        String lastPreview = latest.map(msg -> {
            if (msg.estSysteme() || Boolean.TRUE.equals(msg.getSignalee())) {
                return msg.getContenu();
            }
            return "[Contenu masqué]";
        }).orElse(null);

        return new AdminConversationSummaryResponse(
                conversation.getId(),
                title,
                conversation.estGroupe() ? "groupe" : "directe",
                participants.size(),
                messagesCount,
                activityAt,
                resolveModerationStatus(conversation),
                Boolean.TRUE.equals(conversation.getArchivee()),
                Boolean.TRUE.equals(conversation.getSignalee()),
                lastPreview,
                latest.filter(message -> !message.estSysteme())
                        .map(Message::getSenderId)
                        .map(senderId -> firstNameFor(senderId, usersMap))
                        .orElse(null),
                unreadCount(conversation, user),
                participants,
                sigCount
        );
    }

    private List<ParticipantSummary> resolveParticipantsOrdered(Conversation conversation,
                                                                Map<Long, UserClient.UserProfile> usersMap) {
        List<Long> orderedIds = conversation.getOrdreParticipants() != null && !conversation.getOrdreParticipants().isEmpty()
                ? conversation.getOrdreParticipants()
                : conversation.getParticipantIds();
        return resolveParticipants(orderedIds, usersMap);
    }

    private String resolveConversationLabel(Conversation conversation, List<ParticipantSummary> participants) {
        if (conversation.estGroupe()) {
            if (conversation.getNomGroupe() != null && !conversation.getNomGroupe().isBlank()) {
                return conversation.getNomGroupe().trim();
            }
        }
        String label = participants.stream()
                .map(ParticipantSummary::nom)
                .filter(Objects::nonNull)
                .filter(name -> !name.isBlank())
                .collect(Collectors.joining(" / "));
        return !label.isBlank() ? label : conversation.getId();
    }

    private boolean matchesScope(AdminConversationSummaryResponse row, String scope) {
        String normalizedScope = normalizeScope(scope);
        if (normalizedScope == null || "TOUTES".equals(normalizedScope)) {
            return true;
        }
        if ("GROUPES".equals(normalizedScope)) {
            return "groupe".equalsIgnoreCase(row.type());
        }
        if ("SIGNALEES".equals(normalizedScope) || "SIGNALEE".equals(normalizedScope)) {
            return row.signalee() || row.signalementsCount() > 0;
        }
        if ("DIRECTES".equals(normalizedScope)) {
            return "directe".equalsIgnoreCase(row.type());
        }
        if ("ARCHIVEES".equals(normalizedScope)) {
            return row.archivee();
        }
        if ("ACTIVES".equals(normalizedScope)) {
            return "active".equalsIgnoreCase(row.statut());
        }
        return true;
    }

    private boolean matchesType(AdminConversationSummaryResponse row, String type) {
        if (type == null || type.isBlank()) {
            return true;
        }
        String normalized = normalizeScope(type);
        if ("GROUPE".equals(normalized) || "GROUPES".equals(normalized)) {
            return "groupe".equalsIgnoreCase(row.type());
        }
        if ("DIRECT".equals(normalized) || "DIRECTE".equals(normalized) || "DIRECTES".equals(normalized)) {
            return "directe".equalsIgnoreCase(row.type());
        }
        return true;
    }

    private boolean matchesStatut(AdminConversationSummaryResponse row, String statut) {
        if (statut == null || statut.isBlank()) {
            return true;
        }
        String normalized = normalizeScope(statut);
        return normalized.equalsIgnoreCase(normalizeScope(row.statut()));
    }

    private boolean matchesQuery(AdminConversationSummaryResponse row, String query) {
        if (query == null || query.isBlank()) {
            return true;
        }
        String normalizedQuery = query.trim().toLowerCase(Locale.ROOT);
        if (containsIgnoreCase(row.id(), normalizedQuery)
                || containsIgnoreCase(row.conversation(), normalizedQuery)
                || containsIgnoreCase(row.dernierMessageContenu(), normalizedQuery)
                || containsIgnoreCase(row.statut(), normalizedQuery)) {
            return true;
        }
        return row.participants() != null && row.participants().stream().anyMatch(participant ->
                containsIgnoreCase(participant.nom(), normalizedQuery)
                        || containsIgnoreCase(participant.role(), normalizedQuery));
    }

    private boolean containsIgnoreCase(String value, String query) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(query);
    }

    private String normalizeScope(String scope) {
        if (scope == null || scope.isBlank()) {
            return null;
        }
        return scope.trim()
                .toUpperCase(Locale.ROOT)
                .replace('É', 'E')
                .replace('È', 'E');
    }

    private String resolveModerationStatus(Conversation conversation) {
        if (Boolean.TRUE.equals(conversation.getArchivee())) {
            return "archivee";
        }
        if (Boolean.TRUE.equals(conversation.getSignalee())) {
            return "signalee";
        }
        return "active";
    }

    private String csvValue(String value) {
        String safe = value == null ? "" : value;
        return "\"" + safe.replace("\"", "\"\"") + "\"";
    }

    private void ensureAdmin(AuthenticatedUser user) {
        if (user == null || !user.isAdmin()) {
            throw new AccessForbiddenException("Cette fonctionnalité est réservée aux administrateurs.");
        }
    }

    private void checkManager(Conversation conversation, AuthenticatedUser user) {
        if (!user.isAdmin() && !conversation.getCandidatUserId().equals(user.id())) {
            throw new AccessForbiddenException("Seul le créateur de la conversation peut effectuer cette action.");
        }
    }

    private void checkParticipant(Conversation conversation, AuthenticatedUser user) {
        if (!user.isAdmin() && !conversation.estParticipant(user.id())) {
            throw new AccessForbiddenException("Vous n'êtes pas participant de cette conversation.");
        }
    }

    private Conversation getConversationOrThrow(String id) {
        return conversationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation introuvable : " + id));
    }

    private ConversationResponse toResponse(Conversation c, AuthenticatedUser user, Map<Long, UserClient.UserProfile> usersMap) {
        Optional<Message> latest = latestMessage(c.getId());
        String latestContent = latest.map(Message::getContenu).orElse(null);
        Long latestSenderId = latest.map(Message::getSenderId).orElse(null);
        String latestSenderNom = latestSenderId != null && latest.map(m -> !m.estSysteme()).orElse(false)
                ? firstNameFor(latestSenderId, usersMap)
                : null;

        boolean estGroupe = c.estGroupe();
        String type = estGroupe ? "groupe" : "direct";

        return new ConversationResponse(
                c.getId(),
                c.getParticipantIds(),
                c.getCandidatUserId(),
                c.getNomGroupe(),
                type,
                estGroupe,
                c.getDateCreation(),
                c.getDernierMessageAt(),
                latestContent,
                latestSenderId,
                latestSenderNom,
                unreadCount(c, user),
                resolveParticipants(c.getParticipantIds(), usersMap)
        );
    }

    private Optional<Message> latestMessage(String conversationId) {
        if (messageRepository == null) {
            return Optional.empty();
        }
        Optional<Message> latest = messageRepository.findFirstByConversationIdOrderByDateDesc(conversationId);
        return latest != null ? latest : Optional.empty();
    }

    private long unreadCount(Conversation conversation, AuthenticatedUser user) {
        if (messageRepository == null || user == null || user.id() == null) {
            return 0L;
        }
        return messageRepository.countByConversationIdAndLuFalseAndSenderIdNot(conversation.getId(), user.id());
    }

    private List<ParticipantSummary> resolveParticipants(List<Long> ids, Map<Long, UserClient.UserProfile> usersMap) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return ids.stream()
                .map(participantId -> {
                    UserClient.UserProfile profile = usersMap.get(participantId);
                    if (profile != null) {
                        return new ParticipantSummary(participantId, profile.nom(), profile.role());
                    }
                    return new ParticipantSummary(participantId, "Membre", null);
                })
                .toList();
    }

    private AuditLog recordAudit(String conversationId,
                                 String messageId,
                                 String signalementId,
                                 Long acteurId,
                                 String acteurNom,
                                 String acteurRole,
                                 String action,
                                 String details,
                                 LocalDateTime when) {
        AuditLog entry = new AuditLog(
                UUID.randomUUID().toString(),
                conversationId,
                messageId,
                signalementId,
                acteurId != null ? acteurId : 0L,
                acteurNom,
                acteurRole,
                action,
                details,
                when
        );
        if (auditLogRepository != null) {
            return auditLogRepository.save(entry);
        }
        return entry;
    }

    private AuditEntryResponse toAuditEntryResponse(AuditLog log) {
        return new AuditEntryResponse(
                log.getId(),
                log.getConversationId(),
                log.getAction(),
                log.getActeurNom(),
                log.getActeurRole(),
                log.getDetails(),
                log.getDateAction()
        );
    }

    private SignalementResponse toSignalementResponse(Signalement s,
                                                      Message msg,
                                                      Map<Long, UserClient.UserProfile> usersMap,
                                                      boolean includeContent) {
        UserClient.UserProfile reporter = usersMap.get(s.getAuteurSignalementId());
        UserClient.UserProfile sender = usersMap.get(s.getAuteurMessageId());
        UserClient.UserProfile admin = s.getAdminId() != null ? usersMap.get(s.getAdminId()) : null;
        boolean masque = msg != null && Boolean.TRUE.equals(msg.getMasque());
        String content = (includeContent && msg != null) ? msg.getContenu() : null;
        return new SignalementResponse(
                s.getId(),
                s.getConversationId(),
                s.getMessageId(),
                reporter != null ? reporter.nom() : "Membre",
                reporter != null ? reporter.role() : null,
                sender != null ? sender.nom() : "Membre",
                sender != null ? sender.role() : null,
                s.getMotif(),
                s.getCommentaire(),
                s.getStatut(),
                s.getActionAdmin(),
                admin != null ? admin.nom() : (s.getAdminId() != null ? "Administrateur" : null),
                s.getDateSignalement(),
                s.getDateTraitement(),
                content,
                masque
        );
    }

    private String normalizeAccentFree(String value) {
        if (value == null) {
            return "";
        }
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .trim();
    }

    private String toJson(List<Long> ids) {
        try {
            return objectMapper.writeValueAsString(ids);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Erreur de sérialisation JSON", e);
        }
    }
}
