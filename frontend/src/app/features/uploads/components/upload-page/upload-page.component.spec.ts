import { ComponentFixture, TestBed } from '@angular/core/testing';
import { vi } from 'vitest';
import { Subject, of } from 'rxjs';
import { CsvUploadRecord } from '../../models/upload-record';
import { UploadService, UploadStreamEvent } from '../../services/upload.service';
import { UploadPageComponent } from './upload-page.component';

describe('UploadPageComponent', () => {
  let fixture: ComponentFixture<UploadPageComponent>;
  let component: UploadPageComponent;
  let listUploadsSpy: ReturnType<typeof vi.fn>;
  let uploadFileSpy: ReturnType<typeof vi.fn>;

  const initialRecords: CsvUploadRecord[] = [
    {
      id: 1,
      filename: 'january.csv',
      status: 'PENDING',
      s3Key: 'key',
      createdAt: '2024-01-01T00:00:00Z',
      updatedAt: '2024-01-01T00:00:00Z'
    }
  ];

  const updatedRecords: CsvUploadRecord[] = [
    {
      id: 2,
      filename: 'feb.csv',
      status: 'VALIDATED',
      s3Key: 'key-2',
      createdAt: '2024-02-01T00:00:00Z',
      updatedAt: '2024-02-01T00:00:00Z'
    }
  ];

  beforeEach(async () => {
    listUploadsSpy = vi.fn().mockReturnValueOnce(of(initialRecords)).mockReturnValue(of(updatedRecords));
    uploadFileSpy = vi.fn();

    await TestBed.configureTestingModule({
      imports: [UploadPageComponent],
      providers: [{ provide: UploadService, useValue: { listUploads: listUploadsSpy, uploadFile: uploadFileSpy } }]
    }).compileComponents();

    fixture = TestBed.createComponent(UploadPageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('loads upload history on init', () => {
    expect(component.uploads()).toEqual(initialRecords);
    expect(listUploadsSpy).toHaveBeenCalled();
  });

  it('streams progress updates and refreshes history after upload', () => {
    const subject = new Subject<UploadStreamEvent>();
    uploadFileSpy.mockReturnValue(subject.asObservable());

    const file = new File(['id,name'], 'data.csv', { type: 'text/csv' });
    component.selectedFile = file;
    component.upload();

    expect(uploadFileSpy).toHaveBeenCalledWith(file);
    subject.next({ kind: 'progress', progress: 55 });
    expect(component.uploadProgress()).toBe(55);

    subject.next({ kind: 'success', upload: updatedRecords[0] });
    subject.complete();

    expect(component.uploads()).toEqual(updatedRecords);
    expect(component.feedback()?.type).toBe('success');
    expect(component.selectedFile).toBeNull();
  });
});
