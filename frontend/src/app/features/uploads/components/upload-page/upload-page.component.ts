import { CommonModule, DatePipe } from '@angular/common';
import { Component, ElementRef, OnDestroy, OnInit, ViewChild, signal } from '@angular/core';
import { Subscription, finalize } from 'rxjs';
import { CsvUploadRecord, CsvUploadStatus } from '../../models/upload-record';
import { UploadService, UploadStreamEvent } from '../../services/upload.service';

interface FeedbackMessage {
  type: 'success' | 'error';
  message: string;
}

@Component({
  selector: 'app-upload-page',
  standalone: true,
  imports: [CommonModule, DatePipe],
  templateUrl: './upload-page.component.html',
  styleUrl: './upload-page.component.css'
})
export class UploadPageComponent implements OnInit, OnDestroy {
  @ViewChild('fileInput') fileInput?: ElementRef<HTMLInputElement>;
  readonly uploads = signal<CsvUploadRecord[]>([]);
  readonly loadingHistory = signal(false);
  readonly feedback = signal<FeedbackMessage | null>(null);
  readonly uploadProgress = signal<number | null>(null);

  selectedFile: File | null = null;
  uploadInFlight = false;

  private readonly subscriptions: Subscription[] = [];

  constructor(private readonly uploadService: UploadService) {}

  ngOnInit(): void {
    this.loadUploads();
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(sub => sub.unsubscribe());
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.selectedFile = input.files?.[0] ?? null;
    this.feedback.set(null);
  }

  upload(): void {
    if (!this.selectedFile || this.uploadInFlight) {
      return;
    }

    this.uploadInFlight = true;
    this.uploadProgress.set(0);
    this.feedback.set(null);

    const upload$ = this.uploadService
      .uploadFile(this.selectedFile)
      .pipe(finalize(() => (this.uploadInFlight = false)))
      .subscribe({
        next: event => this.handleUploadEvent(event),
        error: () => {
          this.uploadProgress.set(null);
          this.feedback.set({ type: 'error', message: 'Upload failed. Please try again.' });
        }
      });

    this.subscriptions.push(upload$);
  }

  refreshUploads(): void {
    this.loadUploads();
  }

  trackByUploadId(_: number, upload: CsvUploadRecord): number {
    return upload.id;
  }

  statusLabel(status: CsvUploadStatus): string {
    switch (status) {
      case 'PENDING':
        return 'Pending';
      case 'VALIDATING':
        return 'Validating';
      case 'VALIDATED':
        return 'Validated';
      case 'VALIDATION_FAILED':
        return 'Validation failed';
    }
  }

  statusClass(status: CsvUploadStatus): string {
    switch (status) {
      case 'VALIDATED':
        return 'status success';
      case 'VALIDATION_FAILED':
        return 'status error';
      case 'VALIDATING':
        return 'status warning';
      default:
        return 'status info';
    }
  }

  private handleUploadEvent(event: UploadStreamEvent): void {
    if (event.kind === 'progress') {
      this.uploadProgress.set(event.progress);
      return;
    }

    this.uploadProgress.set(null);
    this.feedback.set({
      type: 'success',
      message: `Upload registered! Tracking #${event.upload.id}`
    });
    this.selectedFile = null;
    if (this.fileInput) {
      this.fileInput.nativeElement.value = '';
    }
    this.loadUploads();
  }

  private loadUploads(): void {
    this.loadingHistory.set(true);
    const load$ = this.uploadService
      .listUploads()
      .pipe(finalize(() => this.loadingHistory.set(false)))
      .subscribe({
        next: records => this.uploads.set(records),
        error: () =>
          this.feedback.set({
            type: 'error',
            message: 'Unable to load uploads. Please try again.'
          })
      });

    this.subscriptions.push(load$);
  }
}
