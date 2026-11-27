package tn.esprit.skillmatch.messagerie.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.skillmatch.messagerie.config.AuthenticatedUser;
import tn.esprit.skillmatch.messagerie.config.UserClient;
import tn.esprit.skillmatch.messagerie.dto.CreateSignalementRequest;
import tn.esprit.skillmatch.messagerie.dto.SignalementResponse;
import tn.esprit.skillmatch.messagerie.dto.TraiterSignalementRequest;
import tn.esprit.skillmatch.messagerie.entity.AuditLog;
import tn.esprit.skillmatch.messagerie.entity.Conversation;
import tn.esprit.skillmatch.messagerie.entity.Message;
import tn.esprit.skillmatch.messagerie.entity.Signalement;
import tn.esprit.skillmatch.messagerie.entity.SuspensionEnvoi;
import tn.esprit.skillmatch.messagerie.exception.AccessForbiddenException;
import tn.esprit.skillmatch.messagerie.exception.BadRequestException;
import tn.esprit.skillmatch.messagerie.exception.ConflictException;
import tn.esprit.skillmatch.messagerie.exception.ResourceNotFoundException;
import tn.esprit.skillmatch.messagerie.repository.AuditLogRepository;
import tn.esprit.skillmatch.messagerie.repository.ConversationRepository;
import tn.esprit.skillmatch.messagerie.repository.MessageRepository;
import tn.esprit.skillmatch.messagerie.repository.SignalementRepository;
import tn.esprit.skillmatch.messagerie.repository.SuspensionEnvoiRepository;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class SignalementService {

    private final SignalementRepository signalementRepository;
    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final AuditLogRepository auditLogRepository;
    private final SuspensionEnvoiRepository suspensionEnvoiRepository;
    private final UserClient userClient;

    public SignalementService(SignalementRepository signalementRepository,
                              MessageRepository messageRepository,
                              ConversationRepository conversationRepository,
                              AuditLogRepository auditLogRepository,
                              SuspensionEnvoiRepository suspensionEnvoiRepository,
                              UserClient userClient) {
        this.signalementRepository = signalementRepository;
        this.messageRepository = messageRepository;
        this.conversationRepository = conversationRepository;
        this.auditLogRepository = auditLogRepository;
        this.suspensionEnvoiRepository = suspensionEnvoiRepository;
        this.userClient = userClient;
    }

    @Transactional
    public SignalementResponse signalerMessage(String messageId,
                                               CreateSignalementRequest request,
                                               AuthenticatedUser user) {
        if (user == null || user.id() == null) {
            throw new AccessForbiddenException("Authentification requise.");
        }
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message introuvable : " + messageId));
        Conversation conversation = message.getConversation();

        if (!user.isAdmin() && !conversation.estParticipant(user.id())) {
            throw new AccessForbiddenException("Vous n'êtes pas participant de cette conversation.");
        }
        if (message.estSysteme()) {
            throw new BadRequestException("Un message système ne peut pas être signalé.");
        }
        if (user.id().equals(message.getSenderId())) {
            throw new BadRequestException("Vous ne pouvez pas signaler votre propre message.");
        }
        if (signalementRepository.existsByMessageIdAndAuteurSignalementId(messageId, user.id())) {
            throw new ConflictException("Vous avez déjà signalé ce message.");
        }

        String motif = normalizeMotif(request != null ? request.motif() : null);
        String commentaire = request != null && request.commentaire() != null && !request.commentaire().isBlank()
                ? request.commentaire().trim()
                : null;

        LocalDateTime now = LocalDateTime.now();
        message.setSignalee(true);
        messageRepository.save(message);

        conversation.setSignalee(true);
        if (conversation.getDateSignalement() == null) {
            conversation.setDateSignalement(now);
        }
        conversationRepository.save(conversation);

        Signalement signalement = new Signalement(
                UUID.randomUUID().toString(),
                conversation.getId(),
                message.getId(),
                user.id(),
                message.getSenderId(),
                motif,
                commentaire,
                now
        );
        Signalement saved = signalementRepository.save(signalement);

        Map<Long, UserClient.UserProfile> usersMap = resolveUsers(
                Set.of(user.id(), message.getSenderId()), user);
        UserClient.UserProfile actor = usersMap.get(user.id());
        auditLogRepository.save(new AuditLog(
                UUID.randomUUID().toString(),
                conversation.getId(),
                message.getId(),
                saved.getId(),
                user.id(),
                actor != null ? actor.nom() : "Membre",
                actor != null ? actor.role() : user.role(),
                "SIGNALEMENT_CREE",
                "Signalement créé — Motif : " + motif,
                now
        ));

        return toResponse(saved, message, usersMap, false);
    }

    @Transactional(readOnly = true)
    public List<SignalementResponse> listForAdmin(String statut, AuthenticatedUser user) {
        ensureAdmin(user);
        List<Signalement> items = (statut != null && !statut.isBlank())
                ? signalementRepository.findByStatutOrderByDateSignalementDesc(statut.trim().toUpperCase(Locale.ROOT))
                : signalementRepository.findAllByOrderByDateSignalementDesc();
        if (items.isEmpty()) {
            return List.of();
        }
        Set<Long> userIds = collectUserIds(items);
        Map<Long, UserClient.UserProfile> usersMap = resolveUsers(userIds, user);
        return items.stream()
                .map(s -> {
                    Message msg = messageRepository.findById(s.getMessageId()).orElse(null);
                    return toResponse(s, msg, usersMap, false);
                })
                .toList();
    }

    @Transactional
    public SignalementResponse consulterMessageSignale(String signalementId, AuthenticatedUser user) {
        ensureAdmin(user);
        Signalement signalement = signalementRepository.findById(signalementId)
                .orElseThrow(() -> new ResourceNotFoundException("Signalement introuvable : " + signalementId));
        Message message = messageRepository.findById(signalement.getMessageId())
                .orElseThrow(() -> new ResourceNotFoundException("Message signalé introuvable."));

        Set<Long> userIds = collectUserIds(List.of(signalement));
        userIds.add(user.id());
        Map<Long, UserClient.UserProfile> usersMap = resolveUsers(userIds, user);
        UserClient.UserProfile adminProfile = usersMap.get(user.id());

        auditLogRepository.save(new AuditLog(
                UUID.randomUUID().toString(),
                signalement.getConversationId(),
                signalement.getMessageId(),
                signalement.getId(),
                user.id(),
                adminProfile != null ? adminProfile.nom() : "Administrateur",
                "ADMIN",
                "CONSULTATION_MESSAGE_SIGNALE",
                "Consultation du contenu du message signalé (motif : " + signalement.getMotif() + ")",
                LocalDateTime.now()
        ));

        return toResponse(signalement, message, usersMap, true);
    }

    @Transactional
    public SignalementResponse traiterSignalement(String signalementId,
                                                  TraiterSignalementRequest request,
                                                  AuthenticatedUser user) {
        ensureAdmin(user);
        Signalement signalement = signalementRepository.findById(signalementId)
                .orElseThrow(() -> new ResourceNotFoundException("Signalement introuvable : " + signalementId));

        String action = normalizeAction(request != null ? request.action() : null);
        String note = request != null && request.noteAdmin() != null && !request.noteAdmin().isBlank()
                ? request.noteAdmin().trim()
                : null;
        LocalDateTime now = LocalDateTime.now();

        Message message = messageRepository.findById(signalement.getMessageId()).orElse(null);
        Conversation conversation = conversationRepository.findById(signalement.getConversationId()).orElse(null);

        switch (action) {
            case Signalement.ACTION_IGNORER -> {
                signalement.setStatut(Signalement.STATUT_IGNORE);
            }
            case Signalement.ACTION_AVERTIR -> {
                signalement.setStatut(Signalement.STATUT_AVERTI);
            }
            case Signalement.ACTION_MASQUER_MESSAGE -> {
                if (message != null) {
                    message.setMasque(true);
                    messageRepository.save(message);
                }
                signalement.setStatut(Signalement.STATUT_MESSAGE_MASQUE);
            }
            case Signalement.ACTION_SUSPENDRE_ENVOI -> {
                if (!suspensionEnvoiRepository.existsByUserIdAndActiveTrue(signalement.getAuteurMessageId())) {
                    suspensionEnvoiRepository.save(new SuspensionEnvoi(
                            UUID.randomUUID().toString(),
                            signalement.getAuteurMessageId(),
                            null,
                            note != null ? note : "Suspension suite à un signalement (" + signalement.getMotif() + ")",
                            user.id(),
                            now
                    ));
                }
                signalement.setStatut(Signalement.STATUT_ENVOI_SUSPENDU);
            }
            default -> throw new BadRequestException("Action admin invalide : " + action);
        }

        signalement.setActionAdmin(action);
        signalement.setAdminId(user.id());
        signalement.setNoteAdmin(note);
        signalement.setDateTraitement(now);
        Signalement saved = signalementRepository.save(signalement);

        if (conversation != null && Signalement.ACTION_IGNORER.equals(action)) {
            long remaining = signalementRepository.countByConversationIdAndStatut(
                    conversation.getId(), Signalement.STATUT_EN_ATTENTE);
            if (remaining == 0L) {
                conversation.setSignalee(false);
                conversation.setDateSignalement(null);
                conversationRepository.save(conversation);
            }
        }

        Set<Long> userIds = collectUserIds(List.of(saved));
        userIds.add(user.id());
        Map<Long, UserClient.UserProfile> usersMap = resolveUsers(userIds, user);
        UserClient.UserProfile adminProfile = usersMap.get(user.id());

        auditLogRepository.save(new AuditLog(
                UUID.randomUUID().toString(),
                saved.getConversationId(),
                saved.getMessageId(),
                saved.getId(),
                user.id(),
                adminProfile != null ? adminProfile.nom() : "Administrateur",
                "ADMIN",
                "SIGNALEMENT_TRAITE",
                "Action : " + actionLabel(action) + (note != null ? " — " + note : ""),
                now
        ));

        return toResponse(saved, message, usersMap, false);
    }

    public SignalementResponse toResponse(Signalement s,
                                          Message msg,
                                          Map<Long, UserClient.UserProfile> usersMap,
                                          boolean includeContent) {
        UserClient.UserProfile reporter = usersMap.get(s.getAuteurSignalementId());
        UserClient.UserProfile sender = usersMap.get(s.getAuteurMessageId());
        UserClient.UserProfile admin = s.getAdminId() != null ? usersMap.get(s.getAdminId()) : null;

        boolean masque = msg != null && Boolean.TRUE.equals(msg.getMasque());
        String content = null;
        if (includeContent && msg != null) {
            content = msg.getContenu();
        }

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

    private Set<Long> collectUserIds(List<Signalement> items) {
        Set<Long> ids = new LinkedHashSet<>();
        for (Signalement s : items) {
            if (s.getAuteurSignalementId() != null) ids.add(s.getAuteurSignalementId());
            if (s.getAuteurMessageId() != null) ids.add(s.getAuteurMessageId());
            if (s.getAdminId() != null) ids.add(s.getAdminId());
        }
        return ids;
    }

    private Map<Long, UserClient.UserProfile> resolveUsers(Set<Long> ids, AuthenticatedUser user) {
        if (userClient == null || ids == null || ids.isEmpty()) {
            return Map.of();
        }
        return userClient.resolveUsers(ids, user != null ? user.token() : null);
    }

    private void ensureAdmin(AuthenticatedUser user) {
        if (user == null || !user.isAdmin()) {
            throw new AccessForbiddenException("Cette fonctionnalité est réservée aux administrateurs.");
        }
    }

    private String normalizeMotif(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new BadRequestException("Le motif du signalement est obligatoire.");
        }
        String norm = Normalizer.normalize(raw, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .trim();
        return switch (norm) {
            case "langage inaproprie", "langage inapproprie", "langage_inapproprie" -> "Langage inapproprié";
            case "spam" -> "Spam";
            case "harcelement" -> "Harcèlement";
            case "autre" -> "Autre";
            default -> throw new BadRequestException(
                    "Motif invalide. Valeurs acceptées : Langage inapproprié, Spam, Harcèlement, Autre.");
        };
    }

    private String normalizeAction(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new BadRequestException("L'action admin est obligatoire.");
        }
        String norm = raw.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
        return switch (norm) {
            case Signalement.ACTION_IGNORER, "IGNORE" -> Signalement.ACTION_IGNORER;
            case Signalement.ACTION_AVERTIR -> Signalement.ACTION_AVERTIR;
            case Signalement.ACTION_MASQUER_MESSAGE, "MASQUER" -> Signalement.ACTION_MASQUER_MESSAGE;
            case Signalement.ACTION_SUSPENDRE_ENVOI, "SUSPENDRE" -> Signalement.ACTION_SUSPENDRE_ENVOI;
            default -> throw new BadRequestException(
                    "Action invalide. Actions acceptées : IGNORER, AVERTIR, MASQUER_MESSAGE, SUSPENDRE_ENVOI.");
        };
    }

    private String actionLabel(String action) {
        return switch (action) {
            case Signalement.ACTION_IGNORER -> "Signalement ignoré";
            case Signalement.ACTION_AVERTIR -> "Utilisateur averti";
            case Signalement.ACTION_MASQUER_MESSAGE -> "Message masqué";
            case Signalement.ACTION_SUSPENDRE_ENVOI -> "Envoi de messages suspendu";
            default -> action;
        };
    }
}
