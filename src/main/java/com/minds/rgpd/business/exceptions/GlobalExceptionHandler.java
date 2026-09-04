package com.minds.rgpd.business.exceptions;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.io.IOException;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<String> handleNotFound(ResourceNotFoundException e) {
        log.error("Ressource non trouvée : {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<String> handleDuplicate(DuplicateResourceException e) {
        log.warn("Doublon : {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
    }

    @ExceptionHandler(InvalidFileException.class)
    public ResponseEntity<ProblemDetail> handleInvalidFile(InvalidFileException e) {
        log.warn("Fichier invalide : {}", e.getMessage());
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        pd.setTitle("Fichier invalide");
        pd.setDetail(e.getMessage());
        return ResponseEntity.badRequest().body(pd);
    }

    /**
     * Dépassement détecté par le conteneur pendant la lecture du flux, avant que
     * le contrôleur ne soit atteint. Sans ce handler, Spring répondrait 500.
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ProblemDetail> handleTooLarge(MaxUploadSizeExceededException e) {
        log.warn("Fichier trop volumineux : {}", e.getMessage());
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.CONTENT_TOO_LARGE);
        pd.setTitle("Fichier trop volumineux");
        pd.setDetail("Le fichier envoyé dépasse la taille maximale autorisée.");
        return ResponseEntity.status(HttpStatus.CONTENT_TOO_LARGE).body(pd);
    }

    /**
     * Violations de @Valid sur un corps de requête. Sans ce handler, Spring
     * répondrait dans son propre format, incohérent avec les ProblemDetail
     * renvoyés par le reste de l'API.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleInvalidBody(MethodArgumentNotValidException ex) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        pd.setTitle("Requête invalide");
        pd.setDetail(ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fe -> fe.getField() + " " + fe.getDefaultMessage())
                .collect(Collectors.joining(", ")));
        return ResponseEntity.badRequest().body(pd);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleConstraintViolation(ConstraintViolationException ex) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        pd.setTitle("Paramètre invalide");
        pd.setDetail(ex.getConstraintViolations()
                .stream()
                .map(cv -> {
                    String path = cv.getPropertyPath().toString();
                    int dot = path.lastIndexOf('.');
                    return (dot >= 0 ? path.substring(dot + 1) : path) + " " + cv.getMessage();
                })
                .collect(Collectors.joining(", ")));
        return ResponseEntity.badRequest().body(pd);
    }

    /**
     * Requête recevable mais inexploitable (client introuvable dans le jeton,
     * fichier au format inattendu…) : 400 plutôt que 500.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("Requête invalide : {}", e.getMessage());
        return ResponseEntity.badRequest().body(e.getMessage());
    }

    @ExceptionHandler(IOException.class)
    public ResponseEntity<String> handleIOException(IOException ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ex.getMessage());
    }
}
